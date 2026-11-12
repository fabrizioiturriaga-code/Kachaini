package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import kanichi.dto.UsuarioSesion;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.EstudianteRepository;
import kanichi.security.AvatarSesionListener;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

class AvatarSesionListenerTest {
  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final AvatarSesionListener listener = new AvatarSesionListener(repository);

  @Test
  void alIniciarSesionElEstudianteTieneSuAvatar() {
    Estudiante estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    estudiante.actualizarAvatar(AvatarEstudiante.CONDOR);
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
    UsuarioSesion sesion =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);

    listener.alIniciarSesion(evento(sesion));

    assertEquals("🦅", sesion.getAvatar());
  }

  @Test
  void elDocenteNoTieneAvatar() {
    UsuarioSesion sesion =
        new UsuarioSesion(2L, "Luz", "luz@example.org", "hash", Rol.DOCENTE, true, false);

    listener.alIniciarSesion(evento(sesion));

    assertNull(sesion.getAvatar());
    verifyNoInteractions(repository);
  }

  private static AuthenticationSuccessEvent evento(UsuarioSesion sesion) {
    return new AuthenticationSuccessEvent(
        UsernamePasswordAuthenticationToken.authenticated(sesion, null, sesion.getAuthorities()));
  }
}
