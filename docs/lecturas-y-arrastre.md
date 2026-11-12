# Lecturas y arrastrar y soltar — HU-24 y HU-25

## Alcance

- **HU-24.** El estudiante elige un texto en `/estudiante/lecturas`. La lectura muestra el texto completo y sus preguntas en la misma pantalla, con un botón para escucharlo en los navegadores compatibles. Al enviar todas las respuestas se muestra un resumen con aciertos y errores por tema (*Idea principal*, *Datos del texto* e *Inferencia*), la respuesta elegida en cada pregunta y su explicación.
- **HU-25.** El estudiante elige una actividad en `/estudiante/arrastre` y arrastra cada elemento hasta su destino. También puede tocar un elemento y luego el destino, lo que permite usarlo en pantallas táctiles y con teclado. Al verificar, el sistema marca qué emparejamientos son correctos y cuáles no, y conserva lo ubicado para corregir solo los incorrectos.
- Hay una lectura y una actividad por unidad del mapa, y el estudiante solo ve las de su grado. Cada una se habilita cuando su unidad está desbloqueada (HU-18); antes del módulo inicial redirigen a `/onboarding`. Las de otro grado responden HTTP 404.
- Son actividades de práctica: no consumen energía ni modifican el progreso del mapa. Desde HU-16, aprobarlas con 80 % o más otorga recompensas (ver [recompensas-desafios-y-ligas.md](recompensas-desafios-y-ligas.md)).

## Rutas

Todas requieren rol `ESTUDIANTE` (cubierto por `/estudiante/**`).

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/estudiante/lecturas` | Lecturas por unidad, disponibles o bloqueadas. |
| GET | `/estudiante/lecturas/{codigo}` | Texto y preguntas. Unidad bloqueada: redirige a la lista. Código inexistente: HTTP 404. |
| POST | `/estudiante/lecturas/{codigo}` | `_csrf` y `respuestas[i]` (índice de la opción). Si falta alguna, conserva las elegidas y pide completarlas. |
| GET | `/estudiante/arrastre` | Actividades por unidad, disponibles o bloqueadas. |
| GET | `/estudiante/arrastre/{codigo}` | Destinos y elementos. Unidad bloqueada: redirige a la lista. Código inexistente: HTTP 404. |
| POST | `/estudiante/arrastre/{codigo}` | `_csrf` y `respuestas[i]` (índice del elemento ubicado en el destino `i`). Si falta alguno, pide ubicarlos todos. |

Sin JavaScript, cada destino muestra una lista desplegable con los elementos, y la corrección es la misma.

## Componentes

| Componente | Uso |
|---|---|
| `Lectura` | Catálogo de textos con sus preguntas, tema y explicación. |
| `ActividadArrastre` | Catálogo de parejas elemento-destino. `elementosMezclados()` devuelve siempre el mismo orden mezclado. |
| `LecturaService` / `ArrastreService` | Disponibilidad según el mapa y corrección de respuestas. |
| `RespuestasForm` | Respuestas indexadas enviadas por ambos formularios. |
| `estudiante/practica.html` | Lista compartida de lecturas y actividades. |
| `lectura.js` / `arrastre.js` | Lectura en voz alta; arrastre, selección por toque y validación antes de enviar. |

## Persistencia

No requiere migraciones: los catálogos están en código y los resultados no se guardan.
