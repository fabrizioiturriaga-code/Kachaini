package kanichi.repository;

import java.util.List;
import java.util.Optional;
import kanichi.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
  List<Notificacion> findByEstudianteIdOrderByCreadaDesc(Long estudianteId);

  Optional<Notificacion> findByEstudianteIdAndRetoId(Long estudianteId, Long retoId);

  long countByRetoId(Long retoId);

  long countByRetoIdAndLeidaIsNotNull(Long retoId);

  List<Notificacion> findByRetoIdAndCompletadaIsNotNullOrderByCompletadaAsc(Long retoId);

  @Query(
      "select count(n) from Notificacion n, Estudiante e"
          + " where e.id = n.estudianteId and n.retoId = :retoId"
          + " and e.grado = :grado and e.seccion = :seccion and n.completada is null")
  long contarPendientesDelSalon(
      @Param("retoId") Long retoId,
      @Param("grado") Integer grado,
      @Param("seccion") String seccion);
}