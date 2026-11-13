# Reportes del salón y mensajes: HU-19 y HU-20

## Alcance

- **HU-19.** En `/docente/reportes` el docente elige un grado y, opcionalmente, una sección, y genera un reporte del rendimiento general del salón:
  - Indicadores: estudiantes, avance promedio del grado, estudiantes activos en los últimos 7 días, XP de la semana con su variación respecto a los 7 días anteriores, porcentaje de aciertos y racha promedio.
  - Tendencias y observaciones generadas a partir de esos datos.
  - XP del salón por día.
  - Avance por unidad: cuántos la completaron, cuántos la tienen en curso y cuántos no la empezaron.
  - Destacados de la semana y estudiantes que necesitan apoyo (sin XP en 7 días o con menos del 60 % de aciertos desde 5 intentos).
  - Detalle por estudiante.
- El reporte se exporta a CSV (se abre en Excel) y también se puede imprimir o guardar como PDF desde el navegador.
- **HU-20.** El docente escribe a uno o varios estudiantes de un grado, o a todo el salón, desde `/docente/mensajes`. El estudiante ve sus mensajes en `/estudiante/mensajes`, puede responder y también escribir a cualquier docente autorizado. Cada conversación es entre un docente y un estudiante.
- La barra de navegación de ambos roles muestra un contador de mensajes sin leer. Al abrir una conversación, los mensajes recibidos se marcan como leídos y el remitente ve "Leído".
- Los estudiantes siempre se consultan por grado: un reporte o un envío nunca mezcla grados. Si la selección incluye un estudiante de otro grado, el envío se rechaza.

## Rutas

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/docente/reportes?grado=&seccion=` | Reporte del salón; sin `grado` solo muestra el filtro. Sección vacía: todas las secciones del grado. |
| GET | `/docente/reportes/exportar?grado=&seccion=` | Descarga `reporte-<grado><sección>-<fecha>.csv` (UTF-8, separado por `;`). Grado inválido: HTTP 400. |
| GET | `/docente/mensajes?grado=&seccion=` | Conversaciones del docente y, si hay `grado`, los estudiantes del salón para escribirles. |
| POST | `/docente/mensajes` | `_csrf`, `grado`, `seccion`, `estudiantes` (uno o varios) o `todos=true`, y `texto`. |
| GET/POST | `/docente/mensajes/{estudianteId}` | Ver la conversación con un estudiante y responder (`texto`). Estudiante inexistente: HTTP 404. |
| GET | `/estudiante/mensajes` | Conversaciones del estudiante y formulario para escribir a un docente. |
| POST | `/estudiante/mensajes` | `_csrf`, `docente` y `texto`. Redirige a la conversación. |
| GET/POST | `/estudiante/mensajes/{docenteId}` | Ver la conversación con un docente y responder. Docente inexistente: HTTP 404. |

Las rutas `/docente/**` requieren rol `DOCENTE` y las `/estudiante/**`, rol `ESTUDIANTE`.

## Componentes

| Componente | Uso |
|---|---|
| `ReporteSalonService` | Arma el reporte, las observaciones y el CSV. Los valores que empiezan con `=`, `+`, `-` o `@` se exportan con `'` delante para que Excel no los ejecute como fórmulas. |
| `ReporteSalonRepository` | XP por estudiante y del salón en un periodo, y aciertos y errores de cada estudiante. |
| `MensajeriaService` | Bandejas, conversaciones, envíos, validación del grado y conteo de mensajes sin leer. |
| `MensajesAdvice` | Agrega `mensajesSinLeer` al modelo para el contador de la navegación. |
| `fragments/conversacion.html` | Vista de una conversación, compartida por docente y estudiante. |

## Persistencia

`V18__mensajes_directos.sql` crea `mensajes_directos`, con el docente, el estudiante, quién lo envió, el texto (hasta 1000 caracteres), la fecha de envío y la fecha de lectura. Los reportes no crean tablas: usan `xp_ganada`, `intentos_leccion` y el avance de cada estudiante.
