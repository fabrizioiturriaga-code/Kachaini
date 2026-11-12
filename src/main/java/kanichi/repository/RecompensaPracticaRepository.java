package kanichi.repository;

import java.util.List;
import kanichi.model.Coleccionable;
import kanichi.model.RecompensaPractica;
import kanichi.model.TipoPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecompensaPracticaRepository extends JpaRepository<RecompensaPractica, Long> {
  boolean existsByEstudianteIdAndTipoAndCodigo(Long estudianteId, TipoPractica tipo, String codigo);

  boolean existsByEstudianteIdAndColeccionable(Long estudianteId, Coleccionable coleccionable);

  @Query(
      "select r.coleccionable from RecompensaPractica r"
          + " where r.estudianteId = :estudianteId and r.coleccionable is not null")
  List<Coleccionable> coleccionablesDe(@Param("estudianteId") Long estudianteId);
}
