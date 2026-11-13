package kanichi.controller;

import java.util.UUID;
import kanichi.dto.EstadoEnergia;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.UnidadesDocente.EnMapa;
import kanichi.dto.UnidadesDocente.VistaLeccion;
import kanichi.dto.UnidadesDocente.VistaUnidad;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.LeccionDocente;
import kanichi.service.LeccionDocenteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LeccionDocenteController {
  private static final String AL_MODULO_INICIAL = "redirect:/onboarding";
  private static final String AL_MAPA = "redirect:/inicio";

  private final LeccionDocenteService leccionDocenteService;

  public LeccionDocenteController(LeccionDocenteService leccionDocenteService) {
    this.leccionDocenteService = leccionDocenteService;
  }

  @GetMapping("/estudiante/unidades-docente/{id}")
  public String unidad(
      @AuthenticationPrincipal UsuarioSesion usuario, @PathVariable Long id, Model model) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    VistaUnidad vista = leccionDocenteService.obtenerUnidad(usuario.getId(), id);
    model.addAttribute("unidad", vista.unidad());
    model.addAttribute("lecciones", vista.lecciones());
    model.addAttribute("energia", vista.energia());
    return "estudiante/unidad-docente";
  }

  @GetMapping("/estudiante/lecciones-docente/{id}")
  public String leccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    VistaLeccion vista = leccionDocenteService.obtenerLeccion(usuario.getId(), id);
    if (!vista.disponible()) {
      mensajes.addFlashAttribute("error", "Completa primero la lección anterior.");
      return "redirect:/estudiante/unidades-docente/" + vista.unidad().id();
    }
    return mostrar(model, vista.leccion(), vista.unidad(), vista.completada(), vista.energia());
  }

  @PostMapping("/estudiante/lecciones-docente/{id}")
  public String responder(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable Long id,
      @RequestParam(required = false) Integer respuesta,
      @RequestParam UUID intento,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    VistaLeccion vista = leccionDocenteService.obtenerLeccion(usuario.getId(), id);
    LeccionDocente leccion = vista.leccion();
    ResultadoLeccion resultado;
    try {
      resultado = leccionDocenteService.responder(usuario.getId(), id, respuesta, intento);
    } catch (MisionNoPermitidaException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      return AL_MAPA;
    }

    switch (resultado.resultado()) {
      case CORRECTA:
        model.addAttribute("acierto", mensajeDeAcierto(vista.unidad(), resultado));
        model.addAttribute("unidadCompletada", resultado.unidadCompletada());
        model.addAttribute("logrosNuevos", resultado.logrosNuevos());
        agregarRespuestaElegida(model, leccion, respuesta);
        break;
      case YA_COMPLETADA:
        mensajes.addFlashAttribute("mensaje", "Ya habías completado esta lección.");
        return "redirect:/estudiante/unidades-docente/" + vista.unidad().id();
      case SIN_ENERGIA:
        model.addAttribute("error", mensajeSinEnergia(resultado.energia()));
        break;
      case INTENTO_REPETIDO:
        model.addAttribute(
            "error", "Esta respuesta ya fue evaluada. Puedes realizar un nuevo intento.");
        break;
      case INCORRECTA:
        model.addAttribute("error", mensajeDeError(resultado.energia()));
        agregarRespuestaElegida(model, leccion, respuesta);
        break;
    }
    return mostrar(
        model,
        leccion,
        vista.unidad(),
        resultado.resultado() == ResultadoLeccion.Resultado.CORRECTA,
        resultado.energia());
  }

  private static String mostrar(
      Model model,
      LeccionDocente leccion,
      EnMapa unidad,
      boolean completada,
      EstadoEnergia energia) {
    model.addAttribute("leccion", leccion);
    model.addAttribute("unidad", unidad);
    model.addAttribute("completada", completada);
    model.addAttribute("energia", energia);
    if (!completada && !energia.isAgotada()) {
      model.addAttribute("intento", UUID.randomUUID());
    }
    return "estudiante/leccion-docente";
  }

  private static void agregarRespuestaElegida(
      Model model, LeccionDocente leccion, Integer respuesta) {
    var opciones = leccion.getOpciones();
    if (respuesta != null && respuesta >= 0 && respuesta < opciones.size()) {
      model.addAttribute("respuestaElegida", opciones.get(respuesta));
    }
  }

  private static String mensajeDeAcierto(EnMapa unidad, ResultadoLeccion resultado) {
    String mensaje = "¡Correcto! Ganaste " + resultado.xpGanada() + " XP.";
    if (resultado.unidadCompletada()) {
      mensaje += " ¡Completaste la unidad «" + unidad.mapa().titulo() + "»!";
    }
    return mensaje;
  }

  private static String mensajeDeError(EstadoEnergia energia) {
    if (energia.isAgotada()) {
      return "Respuesta incorrecta. " + mensajeSinEnergia(energia);
    }
    return "Respuesta incorrecta. Perdiste 1 de energía; te quedan "
        + energia.actual()
        + " de "
        + energia.maxima()
        + ".";
  }

  private static String mensajeSinEnergia(EstadoEnergia energia) {
    return "Te quedaste sin energía. Espera "
        + energia.minutosParaRecarga()
        + " min para recuperar una unidad o repasa el contenido de la lección.";
  }
}
