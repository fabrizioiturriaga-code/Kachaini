package kanichi.controller;

import jakarta.validation.Valid;
import kanichi.dto.RegistroForm;
import kanichi.exception.RegistroInvalidoException;
import kanichi.repository.UsuarioRepository;
import kanichi.service.AuthService;
import kanichi.validation.PoliticaCorreo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
  private final AuthService authService;
  private final UsuarioRepository usuarioRepository;

  public AuthController(AuthService authService, UsuarioRepository usuarioRepository) {
    this.authService = authService;
    this.usuarioRepository = usuarioRepository;
  }

  @GetMapping("/")
  public String raiz() {
    return "redirect:/login";
  }

  @GetMapping("/login")
  public String login(
      @org.springframework.web.bind.annotation.RequestParam(name = "error", required = false)
          String error,
      Model model) {
    model.addAttribute("errorLogin", error != null);
    return "auth/login";
  }

  @GetMapping("/registro")
  public String registro(Model model) {
    model.addAttribute("registroForm", new RegistroForm());
    return "auth/registro";
  }

  @PostMapping("/registro")
  public String registrar(
      @Valid @ModelAttribute RegistroForm registroForm,
      BindingResult resultado,
      RedirectAttributes mensajesRedireccion) {
    if (resultado.hasErrors()) {
      return mostrarFormularioConErrores(registroForm);
    }

    try {
      authService.registrar(registroForm);
      mensajesRedireccion.addFlashAttribute(
          "mensaje",
          "Registro exitoso. Si eres docente, espera la autorización institucional para ingresar.");
      return "redirect:/login";
    } catch (RegistroInvalidoException exception) {
      exception
          .getErrores()
          .forEach((campo, mensaje) -> resultado.rejectValue(campo, "registro", mensaje));
    } catch (DataIntegrityViolationException exception) {
      mostrarErrorDePersistencia(registroForm, resultado);
    }

    return mostrarFormularioConErrores(registroForm);
  }

  private String mostrarFormularioConErrores(RegistroForm formulario) {
    formulario.setContrasena(null);
    formulario.setConfirmacion(null);
    return "auth/registro";
  }

  private void mostrarErrorDePersistencia(RegistroForm formulario, BindingResult resultado) {
    String correo = PoliticaCorreo.normalizar(formulario.getCorreo());
    if (usuarioRepository.existsByCorreo(correo)) {
      resultado.rejectValue("correo", "duplicado", "El correo ya está registrado.");
    } else {
      resultado.reject("registro", "No se pudo completar el registro. Inténtalo nuevamente.");
    }
  }
}
