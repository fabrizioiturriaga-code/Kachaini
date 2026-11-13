package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import kanichi.service.ForoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForoController {
  private final ForoService foroService;

  public ForoController(ForoService foroService) {
    this.foroService = foroService;
  }

  @GetMapping("/estudiante/foro")
  public String foro(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("foro", foroService.verForo(usuario.getId()));
    model.addAttribute("largoMensaje", ForoService.LARGO_MENSAJE);
    return "estudiante/foro";
  }

  @PostMapping("/estudiante/foro")
  public String publicar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) String texto,
      RedirectAttributes mensajes) {
    try {
      foroService.publicar(usuario.getId(), texto);
      mensajes.addFlashAttribute("mensaje", "Mensaje publicado.");
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      mensajes.addFlashAttribute("texto", texto);
    }
    return "redirect:/estudiante/foro";
  }
}