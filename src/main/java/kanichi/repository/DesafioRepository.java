package kanichi.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kanichi.model.Desafio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesafioRepository extends JpaRepository<Desafio, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Desafio d where d.id = :id")
  Optional<Desafio> bloquearPorId(@Param("id") Long id);

  @Query(
      "select d from Desafio d where (d.creadorId = :id or d.companeroId = :id)"
          + " and d.estado in :estados order by d.creado desc")
  List<Desafio> deEstudiante(
      @Param("id") Long estudianteId, @Param("estados") Collection<Desafio.Estado> estados);

  @Query(
      "select count(d) > 0 from Desafio d where d.estado in :estados and"
          + " ((d.creadorId = :a and d.companeroId = :b) or (d.creadorId = :b and d.companeroId = :a))")
  boolean existeEntre(
      @Param("a") Long a, @Param("b") Long b, @Param("estados") Collection<Desafio.Estado> estados);
}
