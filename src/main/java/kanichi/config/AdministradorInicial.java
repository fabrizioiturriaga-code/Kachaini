package kanichi.config;

import jakarta.validation.Validator;
import kanichi.dto.RegistroForm;
import kanichi.model.Administrador;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.validation.PoliticaCorreo;
import kanichi.validation.ValidadorContrasena;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministradorInicial implements ApplicationRunner {
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;
  private final Validator validator;
  private final String correo;
  private final String contrasena;

  public AdministradorInicial(
      UsuarioRepository usuarios,
      PasswordEncoder encoder,
      Validator validator,
      @Value("${ADMIN_EMAIL:}") String correo,
      @Value("${ADMIN_PASSWORD:}") String contrasena) {
    this.usuarios = usuarios;
    this.encoder = encoder;
    this.validator = validator;
    this.correo = PoliticaCorreo.normalizar(correo);
    this.contrasena = contrasena;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (correo.isEmpty() && contrasena.isEmpty()) {
      return;
    }
    if (!validator.validateValue(RegistroForm.class, "correo", correo).isEmpty()
        || !ValidadorContrasena.esValida(contrasena)) {
      throw new IllegalStateException("Configura ADMIN_EMAIL y una ADMIN_PASSWORD válida.");
    }
    var existente = usuarios.findByCorreo(correo);
    if (existente.isPresent()) {
      if (existente.get().getRol() != Rol.ADMIN) {
        throw new IllegalStateException("ADMIN_EMAIL ya pertenece a una cuenta de otro rol.");
      }
      return;
    }
    usuarios.saveAndFlush(new Administrador("Administrador", correo, encoder.encode(contrasena)));
  }
}
