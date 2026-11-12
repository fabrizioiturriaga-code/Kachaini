# Contenido de Historia por grado

## Alcance

- Cada grado de secundaria tiene dos unidades de tres lecciones. El contenido es el mismo para todas las secciones de un grado.
- El estudiante solo ve las unidades de su grado en el mapa de aprendizaje (`/inicio`), en las lecturas y en las actividades de arrastrar y soltar.
- Al completar el módulo inicial se desbloquea la primera unidad del grado; al completar sus lecciones, la segunda.
- Una unidad, lección, lectura o actividad de otro grado responde HTTP 404.

## Unidades

| Grado | Unidades | Lectura | Arrastrar y soltar |
|---|---|---|---|
| 1.º | 1. Poblamiento de América y del Perú · 2. Culturas preíncas | `hombre-de-lauricocha` · `caral-ciudad-sagrada` | `lugares-del-poblamiento` · `legado-preinca` |
| 2.º | 3. El Tahuantinsuyo · 4. Conquista del Perú | `los-chasquis` · `encuentro-en-cajamarca` | `organizacion-inca` · `personajes-de-la-conquista` |
| 3.º | 5. Virreinato del Perú · 6. Independencia del Perú | `ciudad-de-los-reyes` · `micaela-bastidas` | `vida-en-el-virreinato` · `fechas-de-la-independencia` |
| 4.º | 7. Primeros años de la República y la era del guano · 8. La Guerra del Pacífico y la Reconstrucción | `las-islas-del-guano` · `el-combate-de-angamos` | `republica-y-guano` · `guerra-del-pacifico` |
| 5.º | 9. El Perú en el siglo XX · 10. El Perú contemporáneo | `la-patria-nueva` · `la-tierra-para-quien-la-trabaja` | `peru-siglo-xx` · `peru-contemporaneo` |

## Componentes

| Componente | Uso |
|---|---|
| `Leccion` | `UNIDADES_POR_GRADO`, `unidadesDelGrado(grado)` y `gradoDeUnidad(unidad)`. La lección anterior solo se busca dentro del mismo grado. |
| `MapaMisionesService` | `obtenerMapa(grado, ...)` devuelve las unidades del grado; `esUltimaDelGrado(unidad)` evita anunciar un nivel de otro grado. |
| `LeccionService` | Usa el grado del estudiante para el mapa, el detalle de unidad, las lecciones y las respuestas. |
| `LecturaService` / `ArrastreService` | Listan y habilitan solo las actividades de las unidades del grado. |

## Persistencia

No requiere migraciones: se usa la columna `grado` de `estudiantes` y los catálogos están en código.
