package kanichi.service;

import kanichi.dto.CambioContrasenaForm;
import kanichi.exception.CambioContrasenaInvalidoException;
import kanichi.model.Usuario;
import kanichi.repository.UsuarioRepository;
import kanichi.validation.ValidadorContrasena;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CambioContrasenaService {
  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public CambioContrasenaService(
      UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public void cambiar(Long usuarioId, CambioContrasenaForm formulario) {
    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado."));

    if (!passwordEncoder.matches(formulario.getContrasenaActual(), usuario.getContrasenaHash())) {
      throw new CambioContrasenaInvalidoException(
          "contrasenaActual", "La contraseña actual es incorrecta.");
    }
    if (!ValidadorContrasena.esValida(formulario.getContrasenaNueva())) {
      throw new CambioContrasenaInvalidoException(
          "contrasenaNueva",
          "Usa al menos 8 caracteres con letras y números, y como máximo 72 bytes UTF-8.");
    }
    if (!formulario.getContrasenaNueva().equals(formulario.getConfirmacion())) {
      throw new CambioContrasenaInvalidoException(
          "confirmacion", "Las contraseñas no coinciden.");
    }

    usuario.actualizarContrasenaHash(passwordEncoder.encode(formulario.getContrasenaNueva()));
    usuarioRepository.save(usuario);
  }
}
