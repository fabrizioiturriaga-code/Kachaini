package kanichi.service;

import kanichi.dto.PerfilEstudianteForm;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.model.Usuario;
import kanichi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilEstudianteService {
  private final UsuarioRepository usuarioRepository;

  public PerfilEstudianteService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Transactional(readOnly = true)
  public Estudiante obtener(Long usuarioId) {
    return buscarEstudiante(usuarioId);
  }

  @Transactional
  public void actualizar(Long usuarioId, PerfilEstudianteForm formulario) {
    Estudiante estudiante = buscarEstudiante(usuarioId);
    estudiante.actualizarNombreVisible(formulario.getNombre().strip());
    estudiante.actualizarAvatar(AvatarEstudiante.valueOf(formulario.getAvatar()));
    usuarioRepository.save(estudiante);
  }

  private Estudiante buscarEstudiante(Long usuarioId) {
    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado."));
    if (!(usuario instanceof Estudiante estudiante)) {
      throw new IllegalStateException("El perfil solicitado no corresponde a un estudiante.");
    }
    return estudiante;
  }
}
