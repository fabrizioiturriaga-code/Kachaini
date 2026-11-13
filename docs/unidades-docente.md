# Unidades y lecciones del docente — HU-27

## Alcance

- En el panel, **📚 Mis unidades** (`/docente/unidades`) lista las unidades del docente y permite crear una nueva: grado, título y su primera lección.
- Cada lección tiene título, contenido (un párrafo por línea), una pregunta con tres opciones y la respuesta correcta. Las tres opciones deben ser distintas.
- En la página de la unidad el docente cambia el título o el grado, agrega lecciones (hasta 10), las edita o las elimina. Una unidad siempre conserva al menos una lección.
- Cada docente solo ve y edita sus propias unidades; las de otro docente responden HTTP 404.
- La unidad aparece en el mapa de aprendizaje (Inicio) **solo para los estudiantes de ese grado**, de todas las secciones, después de las unidades oficiales. Si el docente cambia el grado, la unidad pasa al mapa del nuevo grado.

## Para el estudiante

- Las unidades del docente se habilitan tras el módulo inicial y sus lecciones se completan en orden.
- Funcionan como las lecciones oficiales: una respuesta incorrecta resta energía (HU-12), una correcta otorga 20 XP con el multiplicador vigente (HU-09) y se registra en `xp_ganada` para el ranking semanal. Pueden desbloquear logros por XP.
- Cuentan como tareas en los desafíos (HU-17) y aparecen en el historial de recompensas (HU-16).
- No modifican la racha diaria (HU-11) ni el avance por unidades oficiales del panel de progreso (HU-07), que se calculan con las lecciones oficiales.

## Rutas

| Método | Ruta | Rol | Resultado |
|---|---|---|---|
| GET | `/docente/unidades` | Docente | Lista de unidades y formulario de nueva unidad. |
| POST | `/docente/unidades` | Docente | `grado`, `tituloUnidad` y los campos de la lección. Redirige a la unidad creada. |
| GET / POST | `/docente/unidades/{id}` | Docente | Ver la unidad / guardar `grado` y `tituloUnidad`. |
| POST | `/docente/unidades/{id}/lecciones` | Docente | Agrega una lección al final. |
| GET / POST | `/docente/unidades/{id}/lecciones/{leccionId}` | Docente | Editar una lección. |
| POST | `/docente/unidades/{id}/lecciones/{leccionId}/eliminar` | Docente | Elimina la lección y reordena las demás. |
| GET | `/estudiante/unidades-docente/{id}` | Estudiante | Lecciones de la unidad. Otro grado: HTTP 404. |
| GET / POST | `/estudiante/lecciones-docente/{id}` | Estudiante | Contenido y pregunta / responder con `respuesta` e `intento`. |

Campos de la lección: `titulo` (100), `contenido` (3000), `pregunta` (300), `opcionA`, `opcionB`, `opcionC` (200 cada una) y `respuestaCorrecta` (0, 1 o 2).

## Componentes

| Componente | Uso |
|---|---|
| `UnidadDocenteService` | Crear y editar unidades y lecciones, con validación y control de propiedad. |
| `LeccionDocenteService` | Unidades del mapa por grado, detalle y evaluación de respuestas. |
| `UnidadDocente` / `LeccionDocente` | Entidades guardadas en base de datos (el contenido oficial sigue en `Leccion`). |
| `fragments/leccion-docente.html` | Campos de lección reutilizados al crear, agregar y editar. |

## Persistencia

`V19__unidades_docente.sql` crea `unidades_docente`, `lecciones_docente` y `lecciones_docente_completadas`. La siguiente migración debe ser V20.
