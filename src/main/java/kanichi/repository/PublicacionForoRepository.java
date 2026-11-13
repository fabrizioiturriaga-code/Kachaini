package kanichi.repository;

import java.time.Instant;
import java.util.List;
import kanichi.model.PublicacionForo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicacionForoRepository extends JpaRepository<PublicacionForo, Long> {
  List<PublicacionForo> findTop50ByGradoAndSeccionOrderByCreadaDescIdDesc(
      Integer grado, String seccion);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO publicaciones_foro (grado, seccion, autor_id, tipo, texto, clave, creada)
          VALUES (:grado, :seccion, NULL, :tipo, :texto, :clave, :creada)
          ON CONFLICT (clave) DO NOTHING
          """,
      nativeQuery = true)
  int publicarAutomatica(
      @Param("grado") Integer grado,
      @Param("seccion") String seccion,
      @Param("tipo") String tipo,
      @Param("texto") String texto,
      @Param("clave") String clave,
      @Param("creada") Instant creada);
}