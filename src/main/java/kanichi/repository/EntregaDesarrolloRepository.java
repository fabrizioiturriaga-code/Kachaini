package kanichi.repository;

import java.util.Optional;
import kanichi.model.EntregaDesarrollo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaDesarrolloRepository extends JpaRepository<EntregaDesarrollo, Long> {
  Optional<EntregaDesarrollo> findByEstudianteIdAndLeccion(Long estudianteId, String leccion);
}
