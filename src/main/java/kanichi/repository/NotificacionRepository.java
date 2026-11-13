package kanichi.repository;

import java.util.List;
import java.util.Optional;
import kanichi.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
  List<Notificacion> findByEstudianteIdOrderByCreadaDesc(Long estudianteId);

  Optional<Notificacion> findByEstudianteIdAndRetoId(Long estudianteId, Long retoId);

  long countByRetoId(Long retoId);

  long countByRetoIdAndLeidaIsNotNull(Long retoId);
}