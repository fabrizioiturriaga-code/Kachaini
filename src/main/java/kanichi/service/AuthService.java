package kanichi.service;

import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import kanichi.dto.RegistroForm;
import kanichi.exception.RegistroInvalidoException;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.model.Usuario;
import kanichi.repository.UsuarioRepository;
import kanichi.validation.PoliticaCorreo;
import kanichi.validation.ValidadorContrasena;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final Validator validator;
  private final PoliticaCorreo politicaCorreo;

  public AuthService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      Validator validator,
      PoliticaCorreo politicaCorreo) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.validator = validator;
    this.politicaCorreo = politicaCorreo;
  }

  @Transactional
  public void registrar(RegistroForm formulario) {
    validarFormulario(formulario);

    String correo = PoliticaCorreo.normalizar(formulario.getCorreo());
    verificarCorreoDisponible(correo);

    String contrasenaHash = passwordEncoder.encode(formulario.getContrasena());
    Usuario usuario = crearUsuario(formulario, correo, contrasenaHash);

    usuarioRepository.saveAndFlush(usuario);
  }

  private void validarFormulario(RegistroForm formulario) {
    Map<String, String> errores = new LinkedHashMap<>();
    validator
        .validate(formulario)
        .forEach(
            violacion ->
                errores.put(violacion.getPropertyPath().toString(), violacion.getMessage()));

    validarContrasena(formulario, errores);

    if (formulario.getRol() == Rol.ESTUDIANTE) {
      validarEstudiante(formulario, errores);
    } else if (formulario.getRol() == Rol.DOCENTE) {
      validarDocente(formulario, errores);
    }

    String correo = PoliticaCorreo.normalizar(formulario.getCorreo());
    if (!politicaCorreo.permite(correo)) {
      errores.put("correo", "El dominio no está habilitado por la institución.");
    }

    if (!errores.isEmpty()) {
      throw new RegistroInvalidoException(errores);
    }
  }

  private void validarContrasena(RegistroForm formulario, Map<String, String> errores) {
    if (!ValidadorContrasena.esValida(formulario.getContrasena())) {
      errores.put(
          "contrasena",
          "Usa al menos 8 caracteres con letras y números, y como máximo 72 bytes UTF-8.");
    }
    if (!Objects.equals(formulario.getContrasena(), formulario.getConfirmacion())) {
      errores.put("confirmacion", "Las contraseñas no coinciden.");
    }
  }

  private void validarEstudiante(RegistroForm formulario, Map<String, String> errores) {
    Integer grado = formulario.getGrado();
    if (grado == null || grado < 1 || grado > 5) {
      errores.put("grado", "Selecciona un grado de 1 a 5.");
    }

    String seccion = formulario.getSeccion();
    if (seccion == null || !seccion.matches("[A-F]")) {
      errores.put("seccion", "Selecciona una sección de A a F.");
    }
    if (tieneContenido(formulario.getArea())) {
      errores.put("area", "El área solo corresponde a docentes.");
    }
  }

  private void validarDocente(RegistroForm formulario, Map<String, String> errores) {
    if (!tieneContenido(formulario.getArea())) {
      errores.put("area", "Indica el área docente.");
    }
    if (formulario.getGrado() != null || tieneContenido(formulario.getSeccion())) {
      errores.put("grado", "Un docente no debe enviar grado ni sección.");
    }
  }

  private boolean tieneContenido(String texto) {
    return texto != null && !texto.isBlank();
  }

  private void verificarCorreoDisponible(String correo) {
    if (usuarioRepository.existsByCorreo(correo)) {
      throw new RegistroInvalidoException(Map.of("correo", "El correo ya está registrado."));
    }
  }

  private Usuario crearUsuario(RegistroForm formulario, String correo, String contrasenaHash) {
    String nombre = formulario.getNombre().strip();
    if (formulario.getRol() == Rol.ESTUDIANTE) {
      return new Estudiante(
          nombre, correo, contrasenaHash, formulario.getGrado(), formulario.getSeccion());
    }
    return new Docente(nombre, correo, contrasenaHash, formulario.getArea().strip());
  }
}
