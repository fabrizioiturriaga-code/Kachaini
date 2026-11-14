package kanichi.service;

import java.util.List;
import kanichi.dto.SolicitudDocente;
import kanichi.repository.DocenteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AprobacionDocenteService {
  private final DocenteRepository docentes;
  private final JdbcTemplate jdbc;

  public AprobacionDocenteService(DocenteRepository docentes, JdbcTemplate jdbc) {
    this.docentes = docentes;
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public List<SolicitudDocente> listar(boolean autorizados) {
    return docentes.findByAutorizadoOrderByFechaRegistroAscIdAsc(autorizados).stream()
        .map(d -> new SolicitudDocente(d.getId(), d.getNombre(), d.getCorreo(), d.getArea()))
        .toList();
  }

  @Transactional
  public boolean cambiarAutorizacion(Long administradorId, Long docenteId, boolean autorizado) {
    var docente =
        docentes
            .bloquearPorId(docenteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (docente.isAutorizado() == autorizado) {
      return false;
    }
    jdbc.queryForObject(
        "SELECT set_config('kaichaini.administrador_id', ?, true)",
        String.class,
        administradorId.toString());
    docente.cambiarAutorizacion(autorizado);
    return true;
  }
}
