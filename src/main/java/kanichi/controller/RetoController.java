package kanichi.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import kanichi.dto.ResultadoReto;
import kanichi.dto.UsuarioSesion;
import kanichi.service.ProgresoDocenteService;
import kanichi.service.RetoDocenteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RetoController {
  private final RetoDocenteService retoDocenteService;

  public RetoController(RetoDocenteService retoDocenteService) {
    this.retoDocenteService = retoDocenteService;
  }

  @GetMapping("/docente/retos")
  public String retosDocente(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    agregarFormulario(usuario, model);
    return "docente/retos";
  }

  @PostMapping("/docente/retos")
  public String publicar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      @RequestParam(required = false) String titulo,
      @RequestParam(required = false) String descripcion,
      @RequestParam(required = false) String objetivos,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fechaLimite,
      @RequestParam(required = false) Integer xp,
      @RequestParam(required = false) String pregunta,
      @RequestParam(required = false) String opcion1,
      @RequestParam(required = false) String opcion2,
      @RequestParam(required = false) String opcion3,
      @RequestParam(required = false) String opcion4,
      @RequestParam(required = false) Integer respuestaCorrecta,
      Model model,
      RedirectAttributes mensajes) {
    List<String> opciones = Arrays.asList(opcion1, opcion2, opcion3, opcion4);
    try {
      int notificados =
          retoDocenteService.publicar(
              usuario.getId(),
              grado,
              seccion,
              titulo,
              descripcion,
              objetivos,
              fechaLimite,
              xp,
              pregunta,
              opciones,
              respuestaCorrecta);
      mensajes.addFlashAttribute(
          "mensaje",
          "Reto publicado. Se notificó a "
              + notificados
              + (notificados == 1 ? " estudiante." : " estudiantes."));
      return "redirect:/docente/retos";
    } catch (IllegalArgumentException exception) {
      model.addAttribute("error", exception.getMessage());
      model.addAttribute("grado", grado);
      model.addAttribute("seccion", seccion);
      model.addAttribute("titulo", titulo);
      model.addAttribute("descripcion", descripcion);
      model.addAttribute("objetivos", objetivos);
      model.addAttribute("fechaLimite", fechaLimite);
      model.addAttribute("xp", xp);
      model.addAttribute("pregunta", pregunta);
      model.addAttribute("opciones", opciones);
      model.addAttribute("respuestaCorrecta", respuestaCorrecta);
      agregarFormulario(usuario, model);
      return "docente/retos";
    }
  }

  @GetMapping("/estudiante/avisos")
  public String avisos(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("avisos", retoDocenteService.avisos(usuario.getId()));
    return "estudiante/avisos";
  }

  @GetMapping("/estudiante/avisos/{retoId}")
  public String aviso(
      @AuthenticationPrincipal UsuarioSesion usuario, @PathVariable Long retoId, Model model) {
    model.addAttribute("aviso", retoDocenteService.abrir(usuario.getId(), retoId));
    model.addAttribute("resumenAvisos", retoDocenteService.resumen(usuario.getId()));
    return "estudiante/aviso";
  }

  @PostMapping("/estudiante/avisos/{retoId}/completar")
  public String completar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long retoId,
      @RequestParam(required = false) Integer respuesta,
      RedirectAttributes mensajes) {
    try {
      ResultadoReto resultado = retoDocenteService.completar(usuario.getId(), retoId, respuesta);
      mensajes.addFlashAttribute(resultado.correcto() ? "mensaje" : "error", resultado.mensaje());
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/estudiante/avisos/" + retoId;
  }

  private void agregarFormulario(UsuarioSesion usuario, Model model) {
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("secciones", ProgresoDocenteService.SECCIONES);
    model.addAttribute("xpMaxima", RetoDocenteService.XP_MAXIMA);
    model.addAttribute("hoy", LocalDate.now(RetoDocenteService.ZONA));
    model.addAttribute("retos", retoDocenteService.publicadosPor(usuario.getId()));
  }
}