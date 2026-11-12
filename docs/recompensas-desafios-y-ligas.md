# Recompensas, desafíos y ligas — HU-16, HU-17 y HU-21

## HU-16: recompensas de ejercicios

- Cada envío de una lectura (HU-24) o actividad de arrastre (HU-25) se registra con su calificación.
- Con **80 % o más** de aciertos, la primera aprobación de cada práctica otorga **15 XP**, la insignia «Estudiante destacado» (solo la primera vez) y el **coleccionable** de su unidad si aún no lo tiene. Repetir una práctica aprobada no duplica la recompensa.
- La XP cuenta para el ranking semanal y para los logros existentes (por ejemplo, «Cien puntos»).
- El resultado de la práctica muestra la calificación y la recompensa obtenida.
- **Mi perfil → Mis recompensas** muestra el nivel actual (cada 100 XP se sube un nivel), los coleccionables y el historial de las 30 recompensas más recientes: misiones, lecciones, insignias, prácticas aprobadas, desafíos completados y ascensos de liga.

Con 4 preguntas por práctica, el 80 % equivale a responder todas bien.

## HU-17: desafíos entre estudiantes

`/estudiante/desafios`, enlazado desde Retos y Comunidad.

- Para invitar hay que haber completado al menos una lección o práctica. Se elige un compañero del salón o se escribe el correo de cualquier estudiante del sistema.
- Tipos de desafío (`TipoDesafio`):
  - **Dúo en marcha**: 4 tareas con al menos 80 % de aciertos, 30 XP para cada uno.
  - **Equipo experto**: 8 tareas con al menos 90 % de aciertos, 60 XP para cada uno.
- Estados: **Pendiente** (esperando que el invitado acepte), **Activo** y **Completado**. El invitado puede aceptar o rechazar.
- Desde que se acepta, cuenta lo que hacen ambos: una tarea es una lección completada o una práctica enviada; el porcentaje de aciertos suma intentos de lección y respuestas de prácticas de los dos.
- La página muestra la barra de progreso del reto, cada participante con sus tareas y su porcentaje, y la etiqueta de estado. Al cumplirse la meta, el desafío se completa al abrir la página y ambos reciben la XP y la insignia «Trabajo en equipo».
- Límites: un desafío abierto por pareja y como máximo 3 pendientes o activos por estudiante.

| Método | Ruta | Entrada | Resultado |
|---|---|---|---|
| GET | `/estudiante/desafios` | — | Panel con invitaciones, formulario y desafíos. |
| POST | `/estudiante/desafios` | `_csrf`, `companeroId` o `correo`, `tipo` | Crea la invitación y redirige con confirmación o error. |
| POST | `/estudiante/desafios/{id}/aceptar` | `_csrf` | Solo el invitado; pasa a Activo. |
| POST | `/estudiante/desafios/{id}/rechazar` | `_csrf` | Solo el invitado. |

## HU-21: ascenso y descenso de liga

- La liga se guarda por estudiante (`estudiantes.liga`). Los umbrales son los de `Liga` (Amauta 0, Panaca 40, Auqui 80, Inca 120 XP por semana).
- **Ascenso**: en cuanto la XP de la semana alcanza el mínimo de una liga superior.
- **Descenso**: al terminar una semana (lunes a domingo, hora de Lima) en la que no se alcanzó el mínimo de la liga actual, se baja una liga. Tras varias semanas sin actividad se baja una por semana.
- La evaluación ocurre al abrir Inicio o Comunidad (Comunidad evalúa a todo el salón) y al recibir recompensas, sin tareas programadas.
- Cada cambio se registra en `cambios_liga` y se notifica una sola vez: en Inicio, en Comunidad o en el resultado de la práctica. Comunidad indica además cuánta XP falta para subir o para mantener la liga.

## Componentes

| Componente | Uso |
|---|---|
| `RecompensaService` | Registro de prácticas, recompensas, nivel e historial. |
| `DesafioService` | Creación, aceptación y avance de desafíos; entrega la recompensa al completarse. |
| `LigaService` | Evaluación de ascensos y descensos y notificaciones. `evaluar` se reutiliza al otorgar XP. |
| `ActividadRepository` | Consultas que combinan tablas: historial y avance desde una fecha. |
| `Coleccionable`, `TipoDesafio`, `TipoPractica` | Catálogos en código. |
| `fragments/recompensa.html` | Bloque de recompensa: `~{fragments/recompensa :: bloque(${recompensa})}`. |

## Persistencia

`V14__recompensas_desafios_y_ligas.sql` añade `practicas_realizadas`, `recompensas_practica`, `desafios`, `cambios_liga`, las columnas `estudiantes.liga` y `estudiantes.liga_semana`, y `intentos_leccion.fecha` (los intentos anteriores toman la fecha de la migración). La siguiente migración debe ser V15.
