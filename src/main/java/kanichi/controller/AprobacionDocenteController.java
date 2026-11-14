package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import kanichi.service.AprobacionDocenteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/docentes")
public class AprobacionDocenteController {
  private final AprobacionDocenteService aprobaciones;

  public AprobacionDocenteController(AprobacionDocenteService aprobaciones) {
    this.aprobaciones = aprobaciones;
  }

  @GetMapping
  public String panel(Model model) {
    model.addAttribute("pendientes", aprobaciones.listar(false));
    model.addAttribute("autorizados", aprobaciones.listar(true));
    return "admin/docentes";
  }

  @PostMapping("/{id}/aprobar")
  public String aprobar(
      @AuthenticationPrincipal UsuarioSesion administrador,
      @PathVariable Long id,
      RedirectAttributes mensajes) {
    boolean cambiado = aprobaciones.cambiarAutorizacion(administrador.getId(), id, true);
    mensajes.addFlashAttribute(
        "mensaje", cambiado ? "Cuenta docente aprobada." : "La cuenta docente ya estaba aprobada.");
    return "redirect:/admin/docentes";
  }

  @PostMapping("/{id}/revocar")
  public String revocar(
      @AuthenticationPrincipal UsuarioSesion administrador,
      @PathVariable Long id,
      RedirectAttributes mensajes) {
    boolean cambiado = aprobaciones.cambiarAutorizacion(administrador.getId(), id, false);
    mensajes.addFlashAttribute(
        "mensaje",
        cambiado ? "Autorización docente revocada." : "La cuenta docente ya estaba pendiente.");
    return "redirect:/admin/docentes";
  }
}
