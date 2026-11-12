package kanichi.controller;

import kanichi.dto.RespuestasForm;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Lectura;
import kanichi.service.LecturaService;
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
public class LecturaController {
  private static final String AL_MODULO_INICIAL = "redirect:/onboarding";
  private static final String A_LECTURAS = "redirect:/estudiante/lecturas";

  private final LecturaService lecturaService;

  public LecturaController(LecturaService lecturaService) {
    this.lecturaService = lecturaService;
  }

  @GetMapping("/estudiante/lecturas")
  public String lecturas(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    model.addAttribute("titulo", "Lecturas");
    model.addAttribute(
        "descripcion", "Elige un texto, léelo o escúchalo y responde sus preguntas.");
    model.addAttribute("actividades", lecturaService.listar(usuario.getId()));
    model.addAttribute("rutaBase", "/estudiante/lecturas");
    return "estudiante/practica";
  }

  @GetMapping("/estudiante/lecturas/{codigo}")
  public String lectura(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    Lectura lectura = buscar(codigo);
    if (!lecturaService.estaDisponible(usuario.getId(), lectura)) {
      mensajes.addFlashAttribute("error", "Completa primero la unidad anterior.");
      return A_LECTURAS;
    }
    model.addAttribute("lectura", lectura);
    model.addAttribute("formulario", new RespuestasForm());
    return "estudiante/lectura";
  }

  @PostMapping("/estudiante/lecturas/{codigo}")
  public String responder(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      @ModelAttribute("formulario") RespuestasForm formulario,
      BindingResult enlace,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    Lectura lectura = buscar(codigo);
    if (!lecturaService.estaDisponible(usuario.getId(), lectura)) {
      mensajes.addFlashAttribute("error", "Completa primero la unidad anterior.");
      return A_LECTURAS;
    }
    model.addAttribute("lectura", lectura);
    if (enlace.hasErrors() || !formulario.estaCompleto(lectura.getPreguntas().size())) {
      model.addAttribute("error", "Responde todas las preguntas antes de enviar.");
      return "estudiante/lectura";
    }
    model.addAttribute("resultado", lecturaService.corregir(lectura, formulario.getRespuestas()));
    return "estudiante/lectura";
  }

  private static Lectura buscar(String codigo) {
    return Lectura.porCodigo(codigo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
