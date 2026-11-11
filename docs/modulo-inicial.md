# Módulo inicial — HU-06

## Alcance

- Un estudiante con `onboardingCompletado=false` llega a `/onboarding` tras el login. Ahí ve el módulo inicial: un tutorial de Kaichaini, dos lecciones básicas y un reto con pregunta.
- Las misiones se completan en orden. Cada una otorga XP y algunas una insignia. Repetir una misión no duplica la recompensa.
- Al completar todas, el sistema marca `onboardingCompletado=true`, otorga la insignia «Explorador de Kaichaini», actualiza la sesión y redirige a `/inicio`, donde la unidad 1 del mapa queda desbloqueada.
- Apartados de navegación: **lecciones** en Inicio (mapa de unidades), **logros** en Retos (XP e insignias) y **ranking** en Comunidad (XP del mismo grado y sección; los empates comparten puesto).

## Rutas

| Método | Ruta | Entrada | Resultado |
|---|---|---|---|
| GET | `/onboarding` | Sesión de estudiante | Progreso del módulo, XP y estado de cada misión. |
| GET | `/onboarding/misiones/{codigo}` | Sesión de estudiante | Contenido de la misión. Si está bloqueada, redirige a `/onboarding`; si no existe, HTTP 404. |
| POST | `/onboarding/misiones/{codigo}` | `_csrf` y `respuesta` (índice de la opción, solo en retos) | Éxito: redirige a `/onboarding` con la recompensa, o a `/inicio` al completar el módulo. Respuesta incorrecta: vuelve a mostrar la misión con el error, sin recompensa. |
| GET | `/estudiante/retos` | Sesión de estudiante | Logros obtenidos y por desbloquear, y XP acumulada. |
| GET | `/estudiante/comunidad` | Sesión de estudiante | Ranking del salón (top 10) y puesto propio. |

Códigos de misión: `bienvenida`, `que-es-la-historia`, `linea-de-tiempo`, `reto-de-bienvenida`.

## Componentes

| Componente | Uso |
|---|---|
| `MisionInicial` | Catálogo de misiones: orden, contenido, XP, insignia y pregunta. Agregar una misión aquí la incluye en el módulo. |
| `Logro` | Catálogo de insignias. |
| `ModuloInicialService` | Progreso y completado de misiones. Bloquea la fila del estudiante para evitar recompensas duplicadas por envíos simultáneos. |
| `RankingService` | Ranking por XP del salón. |
| `MapaMisionesService.obtenerMapa(boolean)` | Las unidades permanecen bloqueadas hasta completar el módulo inicial. |

## Persistencia

`V4__modulo_inicial.sql` añade `estudiantes.xp`, `misiones_completadas` y `logros_estudiante`, con clave primaria `(estudiante_id, código)` para impedir duplicados.
