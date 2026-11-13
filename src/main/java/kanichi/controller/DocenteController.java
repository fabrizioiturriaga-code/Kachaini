package kanichi.controller;

import java.util.Arrays;
import java.util.List;
import kanichi.dto.UsuarioSesion;
import kanichi.model.FactorXp;
import kanichi.service.MultiplicadorXpService;
import kanichi.service.ProgresoDocenteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/docente")
public class DocenteController {
  private static final String RETIRAR = "retirar";

  private final ProgresoDocenteService progresoDocenteService;
  private final MultiplicadorXpService multiplicadorXpService;

  public DocenteController(
      ProgresoDocenteService progresoDocenteService,
      MultiplicadorXpService multiplicadorXpService) {
    this.progresoDocenteService = progresoDocenteService;
    this.multiplicadorXpService = multiplicadorXpService;
  }

  @GetMapping("/progreso")
  public String progreso(
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      Model model) {
    agregarSalon(grado, seccion, model);
    return "docente/progreso";
  }

  @GetMapping("/progreso/{id}")
  public String detalle(@PathVariable Long id, Model model) {
    model.addAttribute("detalle", progresoDocenteService.detalle(id));
    return "docente/estudiante";
  }

  @GetMapping("/multiplicadores")
  public String multiplicadores(
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      Model model) {
    agregarSalon(grado, seccion, model);
    model.addAttribute("factores", Arrays.asList(FactorXp.values()));
    model.addAttribute("diasMinimos", MultiplicadorXpService.DIAS_MINIMOS);
    model.addAttribute("diasMaximos", MultiplicadorXpService.DIAS_MAXIMOS);
    return "docente/multiplicadores";
  }

  @PostMapping("/multiplicadores")
  public String guardarMultiplicadores(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam Integer grado,
      @RequestParam(required = false) String seccion,
      @RequestParam(name = "estudiantes", required = false) List<Long> estudiantes,
      @RequestParam(required = false) FactorXp factor,
      @RequestParam(required = false) Integer dias,
      @RequestParam(defaultValue = "asignar") String accion,
      RedirectAttributes mensajes) {
    mensajes.addAttribute("grado", grado);
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    if (seccionValida != null) {
      mensajes.addAttribute("seccion", seccionValida);
    }
    try {
      if (RETIRAR.equals(accion)) {
        int retirados = multiplicadorXpService.retirar(grado, estudiantes);
        mensajes.addFlashAttribute(
            "mensaje",
            retirados == 0
                ? "Los estudiantes seleccionados no tenían un multiplicador asignado."
                : "Se retiró el multiplicador a " + plural(retirados) + ".");
      } else {
        int asignados =
            multiplicadorXpService.asignar(usuario.getId(), grado, estudiantes, factor, dias);
        mensajes.addFlashAttribute(
            "mensaje",
            "Multiplicador "
                + factor.getEtiqueta()
                + " asignado a "
                + plural(asignados)
                + " por "
                + dias
                + (dias == 1 ? " día." : " días."));
      }
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/docente/multiplicadores";
  }

  private void agregarSalon(Integer grado, String seccion, Model model) {
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("secciones", ProgresoDocenteService.SECCIONES);
    model.addAttribute("seccion", seccionValida);
    if (grado == null) {
      return;
    }
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      model.addAttribute("error", "Selecciona un grado de 1 a 5.");
      return;
    }
    model.addAttribute("grado", grado);
    model.addAttribute("estudiantes", progresoDocenteService.listar(grado, seccionValida));
  }

  private static String plural(int cantidad) {
    return cantidad + (cantidad == 1 ? " estudiante" : " estudiantes");
  }
}
