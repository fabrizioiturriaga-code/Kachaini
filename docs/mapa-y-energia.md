# Mapa de aprendizaje y energía — HU-18 y HU-12

## Alcance

- **HU-18.** El mapa de Inicio muestra las 6 unidades con su estado: *completada*, *activa* o *bloqueada*, una barra de avance y estrellas (una por lección completada). Cada unidad tiene 3 lecciones que se completan en orden; al terminar la última, la unidad pasa a *completada*, la siguiente se desbloquea automáticamente y se muestra «¡Nuevo nivel disponible!». Todo el mapa permanece bloqueado hasta completar el módulo inicial (HU-06).
- **HU-12.** Cada estudiante tiene una barra de energía de 5 unidades. Una respuesta incorrecta resta 1 y la barra baja visualmente. Con la energía en cero no se puede avanzar: se muestra un mensaje con el tiempo de espera o la opción de repasar la lección. Se recupera 1 unidad cada 10 minutos hasta el máximo. Una lección ya completada no consume energía.
- Cada lección completada suma su XP (20) y por tanto cuenta para el ranking del salón.

## Rutas

Todas requieren rol `ESTUDIANTE` (ya cubierto por `/estudiante/**`) y haber completado el módulo inicial; si no, redirigen a `/onboarding`.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/inicio` | Mapa de unidades y barra de energía. |
| GET | `/estudiante/unidades/{numero}` | Lecciones de la unidad con su estado. Unidad inexistente: HTTP 404. Bloqueada: redirige a `/inicio`. |
| GET | `/estudiante/lecciones/{codigo}` | Contenido y pregunta. Bloqueada: redirige a `/inicio`. Sin energía: no se muestra el formulario. Código inexistente: HTTP 404. |
| POST | `/estudiante/lecciones/{codigo}` | `_csrf` y `respuesta` (índice de la opción). Correcta: XP y vuelta a la unidad (o a `/inicio` si completó la unidad). Incorrecta: muestra la pista y resta energía. |

## Componentes

| Componente | Uso |
|---|---|
| `Leccion` | Catálogo de lecciones: unidad, contenido, XP y pregunta con explicación. Agregar una lección aquí la incluye en el mapa; cada unidad debe tener `Leccion.LECCIONES_POR_UNIDAD` lecciones. |
| `Estudiante` | `estadoEnergia`, `recargarEnergia` y `perderEnergia` (HU-12); `registrarLeccion` (HU-18). `ENERGIA_MAXIMA` y `RECARGA_POR_UNIDAD` fijan la barra. |
| `LeccionService` | Mapa, detalle de unidad y lección, y evaluación de respuestas. Bloquea la fila del estudiante para que envíos simultáneos no dupliquen XP ni energía. |
| `MapaMisionesService` | Calcula el estado de cada unidad a partir de las lecciones completadas. |
| `fragments/energia.html` | Barra de energía reutilizable: `~{fragments/energia :: barra(${energia})}`. |

## Persistencia

`V10__lecciones_y_energia.sql` añade `estudiantes.energia` y `estudiantes.energia_actualizada` (la energía se calcula al consultarla a partir de estos dos valores, sin tareas programadas) y la tabla `lecciones_completadas` con clave primaria `(estudiante_id, leccion)`.

La versión es `V10` para no chocar con migraciones `V5` a `V9` de otras ramas. Si al integrar ya existe una `V10`, renumerar esta migración.
