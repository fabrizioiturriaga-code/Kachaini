package kanichi.repository;

import java.util.List;
import kanichi.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
  List<Docente> findByAutorizadoTrueOrderByNombreAsc();
}
