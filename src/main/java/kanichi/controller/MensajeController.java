package kanichi.controller;

import java.util.List;
import kanichi.dto.UsuarioSesion;
import kanichi.service.MensajeriaService;
import kanichi.service.ProgresoDocenteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MensajeController {
  private final MensajeriaService mensajeriaService;

  public MensajeController(MensajeriaService mensajeriaService) {
    this.mensajeriaService = mensajeriaService;
  }

  @GetMapping("/docente/mensajes")
  public String bandejaDocente(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      Model model) {
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("secciones", ProgresoDocenteService.SECCIONES);
    model.addAttribute("seccion", seccionValida);
    model.addAttribute("largoMensaje", MensajeriaService.LARGO_MENSAJE);
    model.addAttribute("conversaciones", mensajeriaService.bandejaDocente(usuario.getId()));
    if (grado != null) {
      if (ProgresoDocenteService.esGradoValido(grado)) {
        model.addAttribute("grado", grado);
        model.addAttribute(
            "estudiantes", mensajeriaService.estudiantesDelSalon(grado, seccionValida));
      } else {
        model.addAttribute("error", "Selecciona un grado de 1 a 5.");
      }
    }
    return "docente/mensajes";
  }

  @PostMapping("/docente/mensajes")
  public String enviarDocente(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      @RequestParam(name = "estudiantes", required = false) List<Long> estudiantes,
      @RequestParam(defaultValue = "false") boolean todos,
      @RequestParam(required = false) String texto,
      RedirectAttributes mensajes) {
    if (grado != null) {
      mensajes.addAttribute("grado", grado);
    }
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    if (seccionValida != null) {
      mensajes.addAttribute("seccion", seccionValida);
    }
    try {
      int enviados =
          mensajeriaService.enviarAEstudiantes(
              usuario.getId(), grado, seccionValida, estudiantes, todos, texto);
      mensajes.addFlashAttribute(
          "mensaje",
          "Mensaje enviado a "
              + enviados
              + (enviados == 1 ? " estudiante." : " estudiantes."));
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      mensajes.addFlashAttribute("texto", texto);
    }
    return "redirect:/docente/mensajes";
  }

  @GetMapping("/docente/mensajes/{estudianteId}")
  public String conversacionDocente(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long estudianteId,
      Model model) {
    model.addAttribute(
        "conversacion", mensajeriaService.conversacionDocente(usuario.getId(), estudianteId));
    model.addAttribute("largoMensaje", MensajeriaService.LARGO_MENSAJE);
    return "docente/conversacion";
  }

  @PostMapping("/docente/mensajes/{estudianteId}")
  public String responderDocente(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long estudianteId,
      @RequestParam(required = false) String texto,
      RedirectAttributes mensajes) {
    try {
      mensajeriaService.responderComoDocente(usuario.getId(), estudianteId, texto);
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      mensajes.addFlashAttribute("texto", texto);
    }
    return "redirect:/docente/mensajes/" + estudianteId;
  }

  @GetMapping("/estudiante/mensajes")
  public String bandejaEstudiante(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("conversaciones", mensajeriaService.bandejaEstudiante(usuario.getId()));
    model.addAttribute("docentes", mensajeriaService.docentesDisponibles());
    model.addAttribute("largoMensaje", MensajeriaService.LARGO_MENSAJE);
    return "estudiante/mensajes";
  }

  @PostMapping("/estudiante/mensajes")
  public String escribirDocente(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Long docente,
      @RequestParam(required = false) String texto,
      RedirectAttributes mensajes) {
    try {
      mensajeriaService.enviarComoEstudiante(usuario.getId(), docente, texto);
      mensajes.addFlashAttribute("mensaje", "Mensaje enviado.");
      return "redirect:/estudiante/mensajes/" + docente;
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      mensajes.addFlashAttribute("texto", texto);
      mensajes.addFlashAttribute("docenteElegido", docente);
      return "redirect:/estudiante/mensajes";
    }
  }

  @GetMapping("/estudiante/mensajes/{docenteId}")
  public String conversacionEstudiante(
      @AuthenticationPrincipal UsuarioSesion usuario, @PathVariable Long docenteId, Model model) {
    model.addAttribute(
        "conversacion", mensajeriaService.conversacionEstudiante(usuario.getId(), docenteId));
    model.addAttribute("largoMensaje", MensajeriaService.LARGO_MENSAJE);
    return "estudiante/conversacion";
  }

  @PostMapping("/estudiante/mensajes/{docenteId}")
  public String responderEstudiante(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long docenteId,
      @RequestParam(required = false) String texto,
      RedirectAttributes mensajes) {
    try {
      mensajeriaService.enviarComoEstudiante(usuario.getId(), docenteId, texto);
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      mensajes.addFlashAttribute("texto", texto);
    }
    return "redirect:/estudiante/mensajes/" + docenteId;
  }
}
