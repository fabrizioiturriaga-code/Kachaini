package kanichi.controller;

import kanichi.dto.LeccionDocenteForm;
import kanichi.dto.UsuarioSesion;
import kanichi.model.LeccionDocente;
import kanichi.service.ProgresoDocenteService;
import kanichi.service.UnidadDocenteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/docente/unidades")
public class UnidadDocenteController {
  private final UnidadDocenteService unidadDocenteService;

  public UnidadDocenteController(UnidadDocenteService unidadDocenteService) {
    this.unidadDocenteService = unidadDocenteService;
  }

  @GetMapping
  public String unidades(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("leccion", new LeccionDocenteForm());
    return mostrarUnidades(usuario, model);
  }

  @PostMapping
  public String crear(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String tituloUnidad,
      @ModelAttribute("leccion") LeccionDocenteForm leccion,
      Model model,
      RedirectAttributes mensajes) {
    try {
      Long id = unidadDocenteService.crear(usuario.getId(), grado, tituloUnidad, leccion);
      mensajes.addFlashAttribute(
          "mensaje",
          "Unidad creada. Ya aparece en el mapa de los estudiantes de "
              + grado
              + ".° de secundaria.");
      return "redirect:/docente/unidades/" + id;
    } catch (IllegalArgumentException exception) {
      model.addAttribute("error", exception.getMessage());
      model.addAttribute("grado", grado);
      model.addAttribute("tituloUnidad", tituloUnidad);
      return mostrarUnidades(usuario, model);
    }
  }

  @GetMapping("/{id}")
  public String unidad(
      @AuthenticationPrincipal UsuarioSesion usuario, @PathVariable Long id, Model model) {
    model.addAttribute("nuevaLeccion", new LeccionDocenteForm());
    return mostrarUnidad(usuario, id, model);
  }

  @PostMapping("/{id}")
  public String actualizar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String tituloUnidad,
      RedirectAttributes mensajes) {
    try {
      unidadDocenteService.actualizarUnidad(usuario.getId(), id, grado, tituloUnidad);
      mensajes.addFlashAttribute("mensaje", "Unidad actualizada.");
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/docente/unidades/" + id;
  }

  @PostMapping("/{id}/lecciones")
  public String agregarLeccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @ModelAttribute("nuevaLeccion") LeccionDocenteForm nuevaLeccion,
      Model model,
      RedirectAttributes mensajes) {
    try {
      unidadDocenteService.agregarLeccion(usuario.getId(), id, nuevaLeccion);
      mensajes.addFlashAttribute("mensaje", "Lección agregada.");
      return "redirect:/docente/unidades/" + id;
    } catch (IllegalArgumentException exception) {
      model.addAttribute("errorLeccion", exception.getMessage());
      return mostrarUnidad(usuario, id, model);
    }
  }

  @GetMapping("/{id}/lecciones/{leccionId}")
  public String leccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @PathVariable Long leccionId,
      Model model) {
    LeccionDocente leccion = unidadDocenteService.leccion(usuario.getId(), id, leccionId);
    model.addAttribute("leccion", LeccionDocenteForm.desde(leccion));
    return mostrarLeccion(usuario, id, leccion, model);
  }

  @PostMapping("/{id}/lecciones/{leccionId}")
  public String actualizarLeccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @PathVariable Long leccionId,
      @ModelAttribute("leccion") LeccionDocenteForm leccion,
      Model model,
      RedirectAttributes mensajes) {
    try {
      unidadDocenteService.actualizarLeccion(usuario.getId(), id, leccionId, leccion);
      mensajes.addFlashAttribute("mensaje", "Lección actualizada.");
      return "redirect:/docente/unidades/" + id;
    } catch (IllegalArgumentException exception) {
      model.addAttribute("error", exception.getMessage());
      return mostrarLeccion(
          usuario, id, unidadDocenteService.leccion(usuario.getId(), id, leccionId), model);
    }
  }

  @PostMapping("/{id}/lecciones/{leccionId}/eliminar")
  public String eliminarLeccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @PathVariable Long leccionId,
      RedirectAttributes mensajes) {
    try {
      unidadDocenteService.eliminarLeccion(usuario.getId(), id, leccionId);
      mensajes.addFlashAttribute("mensaje", "Lección eliminada.");
    } catch (IllegalArgumentException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/docente/unidades/" + id;
  }

  private String mostrarUnidades(UsuarioSesion usuario, Model model) {
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("unidades", unidadDocenteService.deDocente(usuario.getId()));
    agregarLimites(model);
    return "docente/unidades";
  }

  private String mostrarUnidad(UsuarioSesion usuario, Long id, Model model) {
    model.addAttribute("unidad", unidadDocenteService.unidad(usuario.getId(), id));
    model.addAttribute("lecciones", unidadDocenteService.lecciones(usuario.getId(), id));
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("maximoLecciones", UnidadDocenteService.MAXIMO_LECCIONES);
    agregarLimites(model);
    return "docente/unidad";
  }

  private String mostrarLeccion(
      UsuarioSesion usuario, Long id, LeccionDocente leccion, Model model) {
    model.addAttribute("unidad", unidadDocenteService.unidad(usuario.getId(), id));
    model.addAttribute("leccionId", leccion.getId());
    model.addAttribute("orden", leccion.getOrden());
    agregarLimites(model);
    return "docente/leccion";
  }

  private static void agregarLimites(Model model) {
    model.addAttribute("maximoTitulo", LeccionDocente.MAXIMO_TITULO);
    model.addAttribute("maximoContenido", LeccionDocente.MAXIMO_CONTENIDO);
    model.addAttribute("maximoPregunta", LeccionDocente.MAXIMO_PREGUNTA);
    model.addAttribute("maximoOpcion", LeccionDocente.MAXIMO_OPCION);
  }
}
