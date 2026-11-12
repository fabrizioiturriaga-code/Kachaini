package kanichi.repository;

import java.util.List;
import kanichi.model.CambioLiga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioLigaRepository extends JpaRepository<CambioLiga, Long> {
  List<CambioLiga> findByEstudianteIdAndNotificadoFalseOrderByIdAsc(Long estudianteId);
}
