package kanichi.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import kanichi.model.Estudiante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReporteSalonRepository extends Repository<Estudiante, Long> {
  @Query(
      value =
          """
          SELECT x.estudiante_id AS id, CAST(SUM(x.cantidad) AS INTEGER) AS puntos
          FROM xp_ganada x
          WHERE x.estudiante_id IN (:ids) AND x.fecha >= :desde AND x.fecha < :hasta
          GROUP BY x.estudiante_id
          """,
      nativeQuery = true)
  List<XpPorEstudiante> xpPorEstudianteEntre(
      @Param("ids") Collection<Long> ids,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  @Query(
      value =
          """
          SELECT CAST(COALESCE(SUM(x.cantidad), 0) AS INTEGER)
          FROM xp_ganada x
          WHERE x.estudiante_id IN (:ids) AND x.fecha >= :desde AND x.fecha < :hasta
          """,
      nativeQuery = true)
  int sumarXpEntre(
      @Param("ids") Collection<Long> ids,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  @Query(
      value =
          """
          SELECT estudiante_id AS id,
                 COUNT(*) FILTER (WHERE correcto IS TRUE) AS aciertos,
                 COUNT(*) FILTER (WHERE correcto IS FALSE) AS errores
          FROM intentos_leccion
          WHERE estudiante_id IN (:ids)
          GROUP BY estudiante_id
          """,
      nativeQuery = true)
  List<IntentosPorEstudiante> intentosPorEstudiante(@Param("ids") Collection<Long> ids);

  interface XpPorEstudiante {
    Long getId();

    Integer getPuntos();
  }

  interface IntentosPorEstudiante {
    Long getId();

    long getAciertos();

    long getErrores();
  }
}
