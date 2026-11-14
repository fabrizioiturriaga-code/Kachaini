package kanichi.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kanichi.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
  List<Docente> findByAutorizadoTrueOrderByNombreAsc();

  List<Docente> findByAutorizadoOrderByFechaRegistroAscIdAsc(boolean autorizado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT d FROM Docente d WHERE d.id = :id")
  Optional<Docente> bloquearPorId(@Param("id") Long id);
}
