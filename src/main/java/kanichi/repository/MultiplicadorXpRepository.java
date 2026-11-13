package kanichi.repository;

import java.util.Collection;
import java.util.List;
import kanichi.model.MultiplicadorXp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MultiplicadorXpRepository extends JpaRepository<MultiplicadorXp, Long> {
  List<MultiplicadorXp> findByEstudianteIdIn(Collection<Long> estudiantes);
}
