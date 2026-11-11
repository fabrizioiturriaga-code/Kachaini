package kanichi.controller;

import jakarta.validation.Valid;
import java.util.Arrays;
import kanichi.dto.CambioContrasenaForm;
import kanichi.dto.PerfilEstudianteForm;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.CambioContrasenaInvalidoException;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.service.CambioContrasenaService;
import kanichi.service.PerfilEstudianteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CuentaController {
  private final PerfilEstudianteService perfilEstudianteService;
  private final CambioContrasenaService cambioContrasenaService;

  public CuentaController(
      PerfilEstudianteService perfilEstudianteService,
      CambioContrasenaService cambioContrasenaService) {
    this.perfilEstudianteService = perfilEstudianteService;
    this.cambioContrasenaService = cambioContrasenaService;
  }

  @GetMapping("/estudiante/perfil")
  public String perfil(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    var estudiante = perfilEstudianteService.obtener(usuario.getId());
    var formulario = new PerfilEstudianteForm();
    formulario.setNombre(estudiante.getNombre());
    formulario.setAvatar(estudiante.getAvatar().name());
    model.addAttribute("perfilForm", formulario);
    agregarDatosPerfil(usuario, estudiante, model);
    return "estudiante/perfil";
  }

  @PostMapping("/estudiante/perfil")
  public String actualizarPerfil(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @Valid @ModelAttribute("perfilForm") PerfilEstudianteForm formulario,
      BindingResult resultado,
      Model model,
      RedirectAttributes mensajes) {
    if (resultado.hasErrors()) {
      agregarDatosPerfil(usuario, perfilEstudianteService.obtener(usuario.getId()), model);
      return "estudiante/perfil";
    }

    perfilEstudianteService.actualizar(usuario.getId(), formulario);
    usuario.actualizarNombreVisible(formulario.getNombre().strip());
    mensajes.addFlashAttribute("mensaje", "Perfil actualizado.");
    return "redirect:/estudiante/perfil";
  }

  private void agregarDatosPerfil(
      UsuarioSesion usuario, Estudiante estudiante, Model model) {
    model.addAttribute("grado", estudiante.getGrado());
    model.addAttribute("seccion", estudiante.getSeccion());
    model.addAttribute("destino", usuario.destino());
    model.addAttribute("avatares", Arrays.asList(AvatarEstudiante.values()));
  }

  @GetMapping("/cuenta/contrasena")
  public String formularioContrasena(
      @AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("cambioContrasenaForm", new CambioContrasenaForm());
    model.addAttribute("destino", usuario.destino());
    return "cuenta/contrasena";
  }

  @PostMapping("/cuenta/contrasena")
  public String cambiarContrasena(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @Valid @ModelAttribute("cambioContrasenaForm") CambioContrasenaForm formulario,
      BindingResult resultado,
      Model model,
      RedirectAttributes mensajes) {
    if (resultado.hasErrors()) {
      limpiarContrasenas(formulario);
      model.addAttribute("destino", usuario.destino());
      return "cuenta/contrasena";
    }

    try {
      cambioContrasenaService.cambiar(usuario.getId(), formulario);
    } catch (CambioContrasenaInvalidoException exception) {
      resultado.rejectValue(exception.getCampo(), "cambio", exception.getMessage());
      limpiarContrasenas(formulario);
      model.addAttribute("destino", usuario.destino());
      return "cuenta/contrasena";
    }

    mensajes.addFlashAttribute("mensaje", "Contraseña actualizada.");
    return "redirect:/cuenta/contrasena";
  }

  private void limpiarContrasenas(CambioContrasenaForm formulario) {
    formulario.setContrasenaActual(null);
    formulario.setContrasenaNueva(null);
    formulario.setConfirmacion(null);
  }
}
