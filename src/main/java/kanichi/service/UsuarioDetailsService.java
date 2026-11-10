package kanichi.service;

import jakarta.validation.Validator;
import kanichi.dto.RegistroForm;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Estudiante;
import kanichi.model.Usuario;
import kanichi.repository.UsuarioRepository;
import kanichi.validation.PoliticaCorreo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
  private final UsuarioRepository usuarioRepository;
  private final Validator validator;

  public UsuarioDetailsService(UsuarioRepository usuarioRepository, Validator validator) {
    this.usuarioRepository = usuarioRepository;
    this.validator = validator;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String correo) {
    String correoNormalizado = PoliticaCorreo.normalizar(correo);
    boolean correoInvalido =
        !validator.validateValue(RegistroForm.class, "correo", correoNormalizado).isEmpty();
    if (correoInvalido) {
      throw new UsernameNotFoundException("Credenciales incorrectas");
    }

    Usuario usuario =
        usuarioRepository
            .findByCorreo(correoNormalizado)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));

    boolean onboardingCompletado = false;
    if (usuario instanceof Estudiante estudiante) {
      onboardingCompletado = estudiante.isOnboardingCompletado();
    }

    return new UsuarioSesion(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        usuario.getContrasenaHash(),
        usuario.getRol(),
        usuario.isAutorizado(),
        onboardingCompletado);
  }
}
