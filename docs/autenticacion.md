# Autenticación — HU-01 y HU-02

## Alcance implementado

- **HU-01:** registro de estudiantes y docentes, validación de datos, correo único normalizado, contraseñas BCrypt y persistencia transaccional.
- **HU-02:** inicio de sesión, autorización por rol, redirección inicial, protección de rutas y cierre de sesión.
- Persistencia PostgreSQL con tablas `usuarios`, `estudiantes` y `docentes`, herencia JPA JOINED y migración `V1__autenticacion.sql`.

## Contratos HTTP

La aplicación utiliza MVC: devuelve HTML o redirecciones, no JSON. Los POST reciben `application/x-www-form-urlencoded`. Todos requieren una sesión con token CSRF válido enviado en el campo `_csrf`; los formularios Thymeleaf lo incluyen mediante `th:action`.

| Método | Ruta | Entrada | Resultado |
|---|---|---|---|
| GET | `/` | Ninguna | Redirección a `/login`. |
| GET | `/registro` | Ninguna | HTML del formulario de registro. |
| POST | `/registro` | Campos de registro y `_csrf` | Éxito: redirección a `/login` con confirmación. Error de validación: HTML del registro con errores, conservando datos no sensibles. |
| GET | `/login` | Ninguna | HTML del formulario de acceso. |
| POST | `/login` | `correo`, `contrasena`, `_csrf` | Éxito: sesión autenticada y redirección por rol. Error: redirección a `/login?error`. |
| POST | `/logout` | `_csrf` y sesión actual | Invalida la sesión y redirige a `/login?logout`. |
| GET | `/onboarding` | Sesión de estudiante | Destino temporal para estudiantes con onboarding pendiente. |
| GET | `/inicio` | Sesión de estudiante | Destino temporal para estudiantes con onboarding completo. |
| GET | `/docente/panel` | Sesión de docente autorizado | Destino temporal para docentes. |

Las redirecciones utilizan HTTP 302; los formularios y destinos HTML correctos responden HTTP 200. Una petición sin CSRF válido recibe HTTP 403. Tras alcanzar el límite temporal, POST `/login` devuelve HTTP 429 y la cabecera `Retry-After` con los segundos de espera; no se consultan las credenciales durante ese intervalo. El acceso anónimo a rutas privadas redirige al login; una sesión con rol incorrecto recibe HTTP 403.

Los tres destinos privados son puntos de integración, no implementaciones de onboarding, navegación ni panel docente.

### Campos de POST /registro

| Campo | Regla |
|---|---|
| `nombre` | Obligatorio, máximo 100 caracteres. |
| `correo` | Obligatorio, formato de email, máximo 254 caracteres; se recortan espacios y se convierte a minúsculas. Debe ser único. |
| `contrasena` | Al menos 8 caracteres, con letras y números; máximo 72 bytes UTF-8. |
| `confirmacion` | Debe coincidir con `contrasena`. |
| `rol` | `ESTUDIANTE` o `DOCENTE`. |
| `grado` | Obligatorio para estudiante: entero entre 1 y 5. No enviar para docente. |
| `seccion` | Obligatoria para estudiante: de `A` a `F`. No enviar para docente. |
| `area` | Obligatoria para docente, máximo 100 caracteres. No enviar para estudiante. |

No se aceptan privilegios adicionales mediante el formulario. Un correo duplicado se informa en el campo `correo`, incluso si dos registros compiten por la misma dirección.

### Sesión y redirecciones

El cliente debe conservar la cookie de sesión `JSESSIONID`. Tras autenticar se rota el identificador; debe utilizarse la cookie actualizada. No se emiten JWT ni tokens de acceso para otros servicios.

- Estudiante con `onboardingCompletado=false`: `/onboarding`.
- Estudiante con `onboardingCompletado=true`: `/inicio`.
- Docente autorizado: `/docente/panel`.

El login normaliza el correo y responde con el mismo mensaje para correo inexistente, contraseña incorrecta y docente pendiente: «Correo o contraseña incorrectos».

Un cliente HTTP que consuma estos formularios debe obtener primero el formulario, conservar sus cookies, extraer `_csrf` y enviar el POST con ambos. Después del login debe renovar el token desde una vista de la sesión autenticada. No existe actualmente un contrato de autenticación entre servicios independiente de esta sesión web.

## Integración con otros módulos

### Identidad del usuario

En un controlador privado, obtener el principal con `@AuthenticationPrincipal UsuarioSesion usuario`:

```java
@GetMapping("/perfil")
public String perfil(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    Long usuarioId = usuario.getId();
    Rol rol = usuario.getRol();
    model.addAttribute("nombre", usuario.getNombre());
    return "perfil";
}
```

Los tipos pertenecen a `kanichi.dto.UsuarioSesion` y `kanichi.model.Rol`. Spring Security verifica la sesión antes de ejecutar controladores privados; en rutas públicas el principal puede ser nulo. Usar el ID del principal para acceder a datos propios, no un ID enviado por el navegador.

### Componentes compartidos

| Componente | Uso por otros módulos |
|---|---|
| `UsuarioRepository` | `findById(id)`, `findByCorreo(correoNormalizado)` y `existsByCorreo(correoNormalizado)`; inyectarlo en servicios. |
| `PoliticaCorreo.normalizar(correo)` | Mantener el mismo formato antes de consultar correos. |
| `PasswordEncoder` | Inyectar el bean existente; `encode` para guardar y `matches` para verificar. |
| `ValidadorContrasena.esValida(valor)` | Reutilizar la regla en el futuro cambio de contraseña. |
| `SecurityConfig` | Añadir permisos por ruta antes de `anyRequest()`. No crear un mecanismo de autenticación paralelo. |
| `Estudiante.onboardingCompletado` | Estado reservado para HU-06; el destino guardado en el principal se calcula al iniciar sesión. Tras cambiarlo, actualizar la identidad de sesión o solicitar un nuevo login. |

HU-03 puede consultar y actualizar el perfil por ID autenticado; HU-05 puede reutilizar la política de contraseña y BCrypt; HU-08 puede consultar nombre y rol para navegación. Cada módulo debe añadir sus propios DTO, controladores, servicios y vistas sin duplicar `Usuario`, su repositorio ni la configuración de seguridad.

Los cambios de datos deben ejecutarse en servicios transaccionales. Añadir migraciones desde `V3__...sql`, sin modificar las ya aplicadas. Coordinar cambios en entidades compartidas, principal de sesión y reglas de permisos.

## Políticas de configuración

- `INSTITUTIONAL_DOMAINS`: dominios admitidos separados por comas. En desarrollo, vacío permite cualquier dominio válido. El perfil `prod` exige una lista no vacía y cookies Secure; activarlo con `SPRING_PROFILES_ACTIVE=prod` y definir los dominios confirmados antes de desplegar.
- `SESSION_COOKIE_SECURE`: `false` para HTTP local; `true` con HTTPS. La cookie es HttpOnly, SameSite=Lax y la sesión expira tras 30 minutos de inactividad.
- Los docentes se guardan con `autorizado=false` y no pueden ingresar hasta su aprobación. No existe una ruta pública de activación. Un operador autorizado debe verificar la identidad y activar el ID correspondiente en la base:

```sql
UPDATE docentes SET autorizado = true WHERE id = :id_docente_verificado;
```

`:id_docente_verificado` representa un parámetro, no SQL listo para pegar. La autorización se carga durante el login y se comprueba de nuevo en cada petición autenticada. Al revocar el permiso, la siguiente petición invalida la sesión y redirige a `/login?revocada`; no se requiere reiniciar la aplicación. La persona responsable de las aprobaciones debe ser designada por el equipo institucional.

## Validación e integración con PostgreSQL

La ejecución local de `mvnw.cmd verify` finalizó correctamente: 41 pruebas, sin fallos, errores ni omisiones, usando PostgreSQL 18.6 en una instancia temporal aislada. Las pruebas cubren servicio de registro, seguridad MVC, límites temporales, política institucional y persistencia real. Incluyen JOINED, rollback, unicidad concurrente, auditoría, revocación de sesiones y registro → login → logout.

Para repetir las pruebas de integración, crear una base exclusiva de pruebas: la suite vacía sus tablas en cada caso.

```powershell
$env:RUN_POSTGRES_TESTS='true'
$env:TEST_DB_URL='jdbc:postgresql://localhost:5432/kaichaini_test'
$env:TEST_DB_USERNAME='kaichaini_app'
$env:TEST_DB_PASSWORD=Read-Host 'Contraseña de pruebas' -MaskInput
.\mvnw.cmd test
```

Sin `TEST_DB_URL`, la suite habilitada utiliza Docker/Testcontainers con PostgreSQL 16. Sin `RUN_POSTGRES_TESTS=true`, solo se ejecutan las pruebas de servicio y MVC.

La versión de Flyway administrada por Spring Boot 3.3.5 advierte que su soporte probado llega a PostgreSQL 16; las verificaciones descritas pasaron en PostgreSQL 18.6 sin modificar las versiones del proyecto.

## Protección y eventos

`LOGIN_MAX_INTENTOS` (5 por defecto) y `LOGIN_ESPERA_SEGUNDOS` (60) limitan fallos por dirección de conexión, dentro de una ventana que comienza con el primer fallo. El bloqueo expira automáticamente; un login correcto no borra los fallos de esa dirección. Se usa `remoteAddr`, no cabeceras enviadas por el cliente. Detrás de un proxy todos pueden compartir dirección: configurar el control en el proxy o adaptar explícitamente una lista de proxies confiables antes de desplegar. El contador está limitado a 10 000 direcciones, vive en memoria y se reinicia con la aplicación; varias instancias requerirán un contador compartido.

Los logs de aplicación emiten `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `LOGIN_LIMITADO`, `LOGOUT` y `SESION_REVOCADA`. Solo los eventos autenticados incluyen ID interno y, en el login, rol; no se registran contraseñas, hashes, correos, cookies ni tokens CSRF.

`V2__auditoria_docente.sql` añade `docentes_autorizaciones`. Un trigger registra cada cambio efectivo de `docentes.autorizado`, incluso mediante SQL: ID docente, nuevo permiso, fecha y usuario de conexión PostgreSQL. Este usuario de BD no identifica por sí solo a la persona operadora; la institución debe mantener esa trazabilidad en su procedimiento de aprobación.

Los mensajes de servidor están en `ValidationMessages.properties` y `messages.properties`. Las vistas relacionan errores y campos mediante ARIA; JavaScript enfoca el primer campo inválido disponible y activa los campos obligatorios según el rol.

## Entorno compartido y CI

La referencia compartida es PostgreSQL 16, sin cambiar las versiones obligatorias de Spring Boot/Flyway. `compose.yaml` permite levantarlo en el puerto local 5433 para no interferir con la instalación existente:

```powershell
$env:DB_PASSWORD=Read-Host 'Contraseña de PostgreSQL local' -MaskInput
docker compose up -d
$env:DB_URL='jdbc:postgresql://localhost:5433/kaichaini'
```

El contenedor necesita Docker y conserva sus datos en un volumen. La instalación PostgreSQL 18 existente no se modifica; mantiene el aviso de compatibilidad de Flyway descrito arriba.

`.github/workflows/verificacion.yml` ejecuta `verify` con Java 17 y un servicio PostgreSQL 16 en cada push y pull request. Las pruebas usan una base desechable. El workflow queda listo para ejecutarse al subir el repositorio; crear el archivo no equivale a haberlo ejecutado en GitHub. Referencia de configuración: [servicios PostgreSQL en GitHub Actions](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers).