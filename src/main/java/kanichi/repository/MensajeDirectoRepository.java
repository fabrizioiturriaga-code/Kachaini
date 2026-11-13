package kanichi.repository;

import java.time.Instant;
import java.util.List;
import kanichi.model.MensajeDirecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeDirectoRepository extends JpaRepository<MensajeDirecto, Long> {
  List<MensajeDirecto> findByDocenteIdAndEstudianteIdOrderByEnviadoAscIdAsc(
      Long docenteId, Long estudianteId);

  List<MensajeDirecto> findByDocenteIdOrderByEnviadoDescIdDesc(Long docenteId);

  List<MensajeDirecto> findByEstudianteIdOrderByEnviadoDescIdDesc(Long estudianteId);

  long countByDocenteIdAndEnviadoPorDocenteFalseAndLeidoIsNull(Long docenteId);

  long countByEstudianteIdAndEnviadoPorDocenteTrueAndLeidoIsNull(Long estudianteId);

  @Modifying
  @Query(
      "update MensajeDirecto m set m.leido = :ahora"
          + " where m.docenteId = :docenteId and m.estudianteId = :estudianteId"
          + " and m.enviadoPorDocente = :enviadoPorDocente and m.leido is null")
  int marcarLeidos(
      @Param("docenteId") Long docenteId,
      @Param("estudianteId") Long estudianteId,
      @Param("enviadoPorDocente") boolean enviadoPorDocente,
      @Param("ahora") Instant ahora);
}
