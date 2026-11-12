# Kaichaini

Plataforma web educativa gamificada para estudiantes y docentes.

## Tecnologías

Java 17, Spring Boot 3.3.5, Spring MVC, Thymeleaf, Spring Data JPA, Spring Security y PostgreSQL 16 como referencia compartida. Compilación con Maven Wrapper y migraciones con Flyway.

## Ejecución local

Crear una base PostgreSQL llamada `kaichaini` y un usuario con permisos sobre ella. Configurar las variables indicadas en `.env.example` en la terminal o en el IDE; el archivo `.env` no se carga automáticamente.

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/kaichaini'
$env:DB_USERNAME='kaichaini_app'
$env:DB_PASSWORD=Read-Host 'Contraseña de PostgreSQL' -MaskInput
.\mvnw.cmd spring-boot:run
```

Opcionalmente, con Docker y `DB_PASSWORD` configurada: `docker compose up -d`. En ese caso, utilizar el puerto `5433` en `DB_URL`.

La aplicación queda disponible en [localhost:8080](http://localhost:8080). Las migraciones se ejecutan al iniciar.

## Compilación y pruebas

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## Organización

- `src/main/java/kanichi`: código de la aplicación, organizado por capas.
- `src/main/resources`: configuración, vistas, estilos y migraciones.
- `src/test/java/kanichi`: pruebas automatizadas.
- `docs`: documentación de implementación e integración.

## Documentación

[Autenticación: HU-01, HU-02 y contratos de integración](docs/autenticacion.md).

[Módulo inicial: HU-06](docs/modulo-inicial.md).

[Mapa de aprendizaje y energía: HU-18 y HU-12](docs/mapa-y-energia.md).
