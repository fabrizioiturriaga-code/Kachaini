package kanichi.repository;

import jakarta.persistence.LockModeType;
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
  /** Bloquea la fila para que dos envíos simultáneos no otorguen la misma recompensa. */
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

  List<Estudiante> findTop10ByGradoAndSeccionOrderByXpDescNombreAsc(Integer grado, String seccion);

  long countByGradoAndSeccionAndXpGreaterThan(Integer grado, String seccion, int xp);
}
