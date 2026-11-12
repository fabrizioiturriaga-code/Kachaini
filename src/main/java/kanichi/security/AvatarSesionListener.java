package kanichi.security;

import kanichi.dto.UsuarioSesion;
import kanichi.model.Rol;
import kanichi.repository.EstudianteRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AvatarSesionListener {
  private final EstudianteRepository estudianteRepository;

  public AvatarSesionListener(EstudianteRepository estudianteRepository) {
    this.estudianteRepository = estudianteRepository;
  }

  @EventListener
  @Transactional(readOnly = true)
  public void alIniciarSesion(AuthenticationSuccessEvent evento) {
    if (evento.getAuthentication().getPrincipal() instanceof UsuarioSesion sesion
        && sesion.getRol() == Rol.ESTUDIANTE) {
      estudianteRepository
          .findById(sesion.getId())
          .ifPresent(estudiante -> sesion.actualizarAvatar(estudiante.getAvatar().getIcono()));
    }
  }
}
