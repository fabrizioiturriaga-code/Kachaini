package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import kanichi.exception.DesafioInvalidoException;
import kanichi.model.TipoDesafio;
import kanichi.service.DesafioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DesafioController {
  private static final String A_DESAFIOS = "redirect:/estudiante/desafios";

  private final DesafioService desafioService;

  public DesafioController(DesafioService desafioService) {
    this.desafioService = desafioService;
  }

  @GetMapping("/estudiante/desafios")
  public String desafios(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("panel", desafioService.panel(usuario.getId()));
    return "estudiante/desafios";
  }

  @PostMapping("/estudiante/desafios")
  public String crear(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Long companeroId,
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) TipoDesafio tipo,
      RedirectAttributes mensajes) {
    try {
      desafioService.crear(usuario.getId(), companeroId, correo, tipo);
      mensajes.addFlashAttribute(
          "mensaje", "Invitación enviada. El desafío empezará cuando tu compañero lo acepte.");
    } catch (DesafioInvalidoException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return A_DESAFIOS;
  }

  @PostMapping("/estudiante/desafios/{id}/aceptar")
  public String aceptar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      RedirectAttributes mensajes) {
    try {
      desafioService.aceptar(usuario.getId(), id);
      mensajes.addFlashAttribute("mensaje", "¡Desafío aceptado! Ya está activo.");
    } catch (DesafioInvalidoException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return A_DESAFIOS;
  }

  @PostMapping("/estudiante/desafios/{id}/rechazar")
  public String rechazar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      RedirectAttributes mensajes) {
    try {
      desafioService.rechazar(usuario.getId(), id);
      mensajes.addFlashAttribute("mensaje", "Invitación rechazada.");
    } catch (DesafioInvalidoException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return A_DESAFIOS;
  }
}
