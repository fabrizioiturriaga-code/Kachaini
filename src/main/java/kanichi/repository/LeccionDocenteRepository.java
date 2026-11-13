package kanichi.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import kanichi.model.LeccionDocente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeccionDocenteRepository extends JpaRepository<LeccionDocente, Long> {
  List<LeccionDocente> findByUnidadIdOrderByOrdenAsc(Long unidadId);

  List<LeccionDocente> findByUnidadIdInOrderByUnidadIdAscOrdenAsc(Collection<Long> unidadIds);

  long countByUnidadId(Long unidadId);

  @Query(
      value =
          """
          SELECT leccion_id FROM lecciones_docente_completadas
          WHERE estudiante_id = :estudianteId
          """,
      nativeQuery = true)
  Set<Long> completadasPor(@Param("estudianteId") Long estudianteId);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO lecciones_docente_completadas (estudiante_id, leccion_id, fecha)
          VALUES (:estudianteId, :leccionId, :fecha)
          ON CONFLICT (estudiante_id, leccion_id) DO NOTHING
          """,
      nativeQuery = true)
  int registrarCompletada(
      @Param("estudianteId") Long estudianteId,
      @Param("leccionId") Long leccionId,
      @Param("fecha") Instant fecha);
}
