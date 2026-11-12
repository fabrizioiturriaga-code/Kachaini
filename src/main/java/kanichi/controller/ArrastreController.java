package kanichi.controller;

import kanichi.dto.RespuestasForm;
import kanichi.dto.ResultadoArrastre;
import kanichi.dto.UsuarioSesion;
import kanichi.model.ActividadArrastre;
import kanichi.model.TipoPractica;
import kanichi.service.ArrastreService;
import kanichi.service.RecompensaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ArrastreController {
  private static final String AL_MODULO_INICIAL = "redirect:/onboarding";
  private static final String A_ACTIVIDADES = "redirect:/estudiante/arrastre";

  private final ArrastreService arrastreService;
  private final RecompensaService recompensaService;

  public ArrastreController(
      ArrastreService arrastreService, RecompensaService recompensaService) {
    this.arrastreService = arrastreService;
    this.recompensaService = recompensaService;
  }

  @GetMapping("/estudiante/arrastre")
  public String actividades(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    model.addAttribute("titulo", "Arrastrar y soltar");
    model.addAttribute(
        "descripcion", "Elige una actividad y lleva cada elemento a su lugar correcto.");
    model.addAttribute("actividades", arrastreService.listar(usuario.getId()));
    model.addAttribute("rutaBase", "/estudiante/arrastre");
    return "estudiante/practica";
  }

  @GetMapping("/estudiante/arrastre/{codigo}")
  public String actividad(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    ActividadArrastre actividad = buscar(codigo);
    if (!arrastreService.estaDisponible(usuario.getId(), actividad)) {
      mensajes.addFlashAttribute("error", "Completa primero la unidad anterior.");
      return A_ACTIVIDADES;
    }
    return mostrar(model, actividad, new RespuestasForm());
  }

  @PostMapping("/estudiante/arrastre/{codigo}")
  public String verificar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      @ModelAttribute("formulario") RespuestasForm formulario,
      BindingResult enlace,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    ActividadArrastre actividad = buscar(codigo);
    if (!arrastreService.estaDisponible(usuario.getId(), actividad)) {
      mensajes.addFlashAttribute("error", "Completa primero la unidad anterior.");
      return A_ACTIVIDADES;
    }
    if (enlace.hasErrors() || !formulario.estaCompleto(actividad.getParejas().size())) {
      model.addAttribute("error", "Ubica todos los elementos antes de verificar.");
      return mostrar(model, actividad, formulario);
    }
    ResultadoArrastre resultado = arrastreService.corregir(actividad, formulario.getRespuestas());
    model.addAttribute("resultado", resultado);
    model.addAttribute(
        "recompensa",
        recompensaService.registrarPractica(
            usuario.getId(),
            TipoPractica.ARRASTRE,
            actividad.getCodigo(),
            actividad.getUnidad(),
            resultado.aciertos(),
            resultado.total()));
    return mostrar(model, actividad, formulario);
  }

  private static String mostrar(
      Model model, ActividadArrastre actividad, RespuestasForm formulario) {
    model.addAttribute("actividad", actividad);
    model.addAttribute("elementos", actividad.elementosMezclados());
    model.addAttribute("formulario", formulario);
    return "estudiante/arrastre";
  }

  private static ActividadArrastre buscar(String codigo) {
    return ActividadArrastre.porCodigo(codigo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
