package kanichi.repository;

import java.util.List;
import kanichi.model.RetoDocente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetoDocenteRepository extends JpaRepository<RetoDocente, Long> {
  List<RetoDocente> findByDocenteIdOrderByPublicadoDesc(Long docenteId);
}