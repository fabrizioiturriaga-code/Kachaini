package kanichi.repository;

import java.util.List;
import kanichi.model.UnidadDocente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadDocenteRepository extends JpaRepository<UnidadDocente, Long> {
  List<UnidadDocente> findByDocenteIdOrderByIdAsc(Long docenteId);

  List<UnidadDocente> findByGradoOrderByIdAsc(int grado);
}
