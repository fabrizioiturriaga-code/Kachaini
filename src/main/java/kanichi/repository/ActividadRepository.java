package kanichi.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActividadRepository {
  public record FilaHistorial(
      String origen, String referencia, Instant fecha, Integer xp, String extra) {}

  public record Avance(long tareas, long aciertos, long intentos) {
    public static final Avance VACIO = new Avance(0, 0, 0);

    public Avance mas(Avance otro) {
      return new Avance(tareas + otro.tareas, aciertos + otro.aciertos, intentos + otro.intentos);
    }

    public int porcentaje() {
      return intentos == 0 ? 0 : (int) (aciertos * 100 / intentos);
    }
  }

  private static final String HISTORIAL =
      """
      SELECT * FROM (
        SELECT 'MISION' AS origen, mision AS referencia, fecha, NULL::INTEGER AS xp,
               NULL AS extra
        FROM misiones_completadas WHERE estudiante_id = ?
        UNION ALL
        SELECT 'LECCION', leccion, fecha, NULL, NULL
        FROM lecciones_completadas WHERE estudiante_id = ?
        UNION ALL
        SELECT 'LECCION_DOCENTE', l.titulo, c.fecha, NULL, NULL
        FROM lecciones_docente_completadas c JOIN lecciones_docente l ON l.id = c.leccion_id
        WHERE c.estudiante_id = ?
        UNION ALL
        SELECT 'INSIGNIA', logro, fecha, NULL, NULL
        FROM logros_estudiante WHERE estudiante_id = ?
        UNION ALL
        SELECT 'PRACTICA', tipo || ':' || codigo, fecha, xp, coleccionable
        FROM recompensas_practica WHERE estudiante_id = ?
        UNION ALL
        SELECT 'DESAFIO', tipo, completado, NULL, NULL
        FROM desafios
        WHERE estado = 'COMPLETADO' AND (creador_id = ? OR companero_id = ?)
        UNION ALL
        SELECT 'LIGA', liga_nueva, fecha, NULL, NULL
        FROM cambios_liga WHERE estudiante_id = ? AND ascenso
      ) h
      ORDER BY fecha DESC
      LIMIT ?
      """;

  private static final String AVANCE =
      """
      SELECT
        (SELECT COUNT(*) FROM lecciones_completadas
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ?)
        + (SELECT COUNT(*) FROM lecciones_docente_completadas
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ?)
        + (SELECT COUNT(*) FROM practicas_realizadas
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ?) AS tareas,
        (SELECT COUNT(*) FROM intentos_leccion
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ? AND correcto IS TRUE)
        + (SELECT COALESCE(SUM(aciertos), 0) FROM practicas_realizadas
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ?) AS aciertos,
        (SELECT COUNT(*) FROM intentos_leccion
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ? AND correcto IS NOT NULL)
        + (SELECT COALESCE(SUM(total), 0) FROM practicas_realizadas
          WHERE estudiante_id = ? AND fecha >= ? AND fecha < ?) AS intentos
      """;

  private final JdbcTemplate jdbc;

  public ActividadRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<FilaHistorial> historial(Long estudianteId, int limite) {
    return jdbc.query(
        HISTORIAL,
        (rs, i) ->
            new FilaHistorial(
                rs.getString("origen"),
                rs.getString("referencia"),
                rs.getTimestamp("fecha").toInstant(),
                (Integer) rs.getObject("xp"),
                rs.getString("extra")),
        estudianteId,
        estudianteId,
        estudianteId,
        estudianteId,
        estudianteId,
        estudianteId,
        estudianteId,
        estudianteId,
        limite);
  }

  public Avance avanceEntre(Long estudianteId, Instant desde, Instant hasta) {
    Timestamp d = Timestamp.from(desde);
    Timestamp h = Timestamp.from(hasta);
    Object[] parametros = new Object[21];
    for (int i = 0; i < 7; i++) {
      parametros[i * 3] = estudianteId;
      parametros[i * 3 + 1] = d;
      parametros[i * 3 + 2] = h;
    }
    return jdbc.queryForObject(
        AVANCE,
        (rs, i) -> new Avance(rs.getLong("tareas"), rs.getLong("aciertos"), rs.getLong("intentos")),
        parametros);
  }

  public boolean tieneActividad(Long estudianteId) {
    Boolean existe =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM lecciones_completadas WHERE estudiante_id = ?)"
                + " OR EXISTS (SELECT 1 FROM lecciones_docente_completadas WHERE estudiante_id = ?)"
                + " OR EXISTS (SELECT 1 FROM practicas_realizadas WHERE estudiante_id = ?)",
            Boolean.class,
            estudianteId,
            estudianteId,
            estudianteId);
    return Boolean.TRUE.equals(existe);
  }
}
