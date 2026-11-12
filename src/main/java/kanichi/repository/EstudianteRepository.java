package kanichi.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kanichi.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
  
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Estudiante e where e.id = :id")
  Optional<Estudiante> bloquearPorId(@Param("id") Long id);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO intentos_leccion (estudiante_id, intento, correcto)
          VALUES (:estudianteId, :intento, :correcto)
          ON CONFLICT (estudiante_id, intento) DO NOTHING
          """,
      nativeQuery = true)
  int registrarIntento(
      @Param("estudianteId") Long estudianteId,
      @Param("intento") UUID intento,
      @Param("correcto") boolean correcto);

  @Query(
      value =
          """
          SELECT COUNT(*) FILTER (WHERE correcto IS TRUE) AS aciertos,
                 COUNT(*) FILTER (WHERE correcto IS FALSE) AS errores
          FROM intentos_leccion
          WHERE estudiante_id = :estudianteId
          """,
      nativeQuery = true)
  BalanceIntentos contarIntentosEvaluados(@Param("estudianteId") Long estudianteId);

  interface BalanceIntentos {
    long getAciertos();

    long getErrores();
  }

  @Modifying
  @Query(
      value =
          """
          INSERT INTO xp_ganada (estudiante_id, cantidad, fecha)
          VALUES (:estudianteId, :cantidad, :fecha)
          """,
      nativeQuery = true)
  int registrarXp(
      @Param("estudianteId") Long estudianteId,
      @Param("cantidad") int cantidad,
      @Param("fecha") Instant fecha);

  @Query(
      value =
          """
          SELECT e.id AS id, u.nombre AS nombre, e.avatar AS avatar,
                 CAST(COALESCE(SUM(x.cantidad), 0) AS INTEGER) AS puntos
          FROM estudiantes e
          JOIN usuarios u ON u.id = e.id
          LEFT JOIN xp_ganada x
                 ON x.estudiante_id = e.id AND x.fecha >= :desde AND x.fecha < :hasta
          WHERE e.grado = :grado AND e.seccion = :seccion
          GROUP BY e.id, u.nombre, e.avatar
          ORDER BY puntos DESC, u.nombre ASC
          """,
      nativeQuery = true)
  List<XpDelPeriodo> xpDelSalonEntre(
      @Param("grado") Integer grado,
      @Param("seccion") String seccion,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  @Query(
      value =
          """
          SELECT CAST(COALESCE(SUM(cantidad), 0) AS INTEGER)
          FROM xp_ganada
          WHERE estudiante_id = :estudianteId AND fecha >= :desde AND fecha < :hasta
          """,
      nativeQuery = true)
  int sumarXpEntre(
      @Param("estudianteId") Long estudianteId,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  List<Estudiante> findByGradoAndSeccionOrderByNombreAsc(Integer grado, String seccion);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Estudiante e where e.grado = :grado and e.seccion = :seccion order by e.id")
  List<Estudiante> bloquearSalon(@Param("grado") Integer grado, @Param("seccion") String seccion);

  @Query(
      "select e.id as id, e.liga as liga from Estudiante e"
          + " where e.grado = :grado and e.seccion = :seccion")
  List<LigaDeEstudiante> ligasDelSalon(
      @Param("grado") Integer grado, @Param("seccion") String seccion);

  interface LigaDeEstudiante {
    Long getId();

    kanichi.model.Liga getLiga();
  }

  interface XpDelPeriodo {
    Long getId();

    String getNombre();

    String getAvatar();

    Integer getPuntos();
  }
}