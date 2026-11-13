# Panel docente: HU-07 y HU-09

## Alcance

- **HU-07.** El docente elige un grado y, opcionalmente, una sección en `/docente/progreso`. Ve una lista con el avance del grado, las misiones completadas, las lecciones completadas, la XP acumulada, la racha activa y el multiplicador vigente de cada estudiante. Al elegir un estudiante ve sus indicadores, el avance por unidad y un gráfico con la XP ganada en cada uno de los últimos 7 días.
- **HU-09.** En `/docente/multiplicadores` el docente elige uno o varios estudiantes de un grado y les asigna un multiplicador de 1.5x, 2x o 3x por 1 a 30 días. Mientras está activo, la XP de cada lección completada se multiplica y se registra en `xp_ganada`, por lo que se refleja en el ranking semanal del salón. El docente puede reemplazarlo asignando otro o quitarlo.
- Los estudiantes siempre se consultan por grado: nunca se mezclan grados distintos en una lista ni en una asignación. Si la selección incluye un estudiante de otro grado, la asignación se rechaza.
- El multiplicador se aplica solo a la XP de las lecciones del mapa, como indica la HU-09.

## Rutas

Todas requieren rol `DOCENTE` (cubierto por `/docente/**`).

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/docente/progreso?grado=&seccion=` | Filtro por grado y sección; sin `grado` solo muestra el filtro. Sección vacía: todas las secciones del grado. |
| GET | `/docente/progreso/{id}` | Indicadores, gráfico semanal y avance por unidad. Estudiante inexistente: HTTP 404. |
| GET | `/docente/multiplicadores?grado=&seccion=` | Estudiantes del salón con su multiplicador actual. |
| POST | `/docente/multiplicadores` | `_csrf`, `grado`, `seccion`, `estudiantes` (uno o varios), `factor` (`X1_5`, `X2`, `X3`), `dias` y `accion` (`asignar` o `retirar`). |

`/docente/panel` sigue siendo el destino de HU-02 y ahora incluye accesos a estas dos secciones.

## Componentes

| Componente | Uso |
|---|---|
| `ProgresoDocenteService` | Indicadores por estudiante y avance de los últimos 7 días a partir de `xp_ganada`. |
| `MultiplicadorXpService` | Asignar, reemplazar y quitar multiplicadores, validando el grado. Implementa `CalculadoraXp`. |
| `CalculadoraXp` | Calcula la XP de una lección. `LeccionService` la usa al registrar una respuesta correcta; sus constructores anteriores usan `SIN_MULTIPLICADOR`. |
| `FactorXp` / `MultiplicadorXp` | Factores permitidos y multiplicador vigente de cada estudiante. |
| `fragments/navegacion-docente.html` | Menú del docente con el mismo estilo que la navegación del estudiante. |

## Persistencia

`V15__multiplicadores_xp.sql` crea `multiplicadores_xp`, con un multiplicador por estudiante, el docente que lo asignó, el factor y el periodo de vigencia.

## Prueba manual

Los docentes se registran con `autorizado=false` (ver [autenticacion.md](autenticacion.md)). Para probar el panel en local, autoriza al docente en la base antes de iniciar sesión.
