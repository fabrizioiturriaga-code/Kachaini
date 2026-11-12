package kanichi.controller;

import java.util.UUID;
import kanichi.dto.EstadoEnergia;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.UsuarioSesion;
import kanichi.dto.VistaLeccion;
import kanichi.dto.VistaUnidad;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.EntregaDesarrollo;
import kanichi.model.Leccion;
import kanichi.service.LeccionService;
import kanichi.service.MapaMisionesService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** HU-18: unidades y lecciones del mapa de aprendizaje. HU-12: energía al fallar. */
@Controller
public class LeccionController {
  private static final String AL_MODULO_INICIAL = "redirect:/onboarding";
  private static final String AL_MAPA = "redirect:/inicio";

  private final LeccionService leccionService;
  private final MapaMisionesService mapaMisionesService;

  public LeccionController(LeccionService leccionService, MapaMisionesService mapaMisionesService) {
    this.leccionService = leccionService;
    this.mapaMisionesService = mapaMisionesService;
  }

  @GetMapping("/estudiante/unidades/{numero}")
  public String unidad(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable int numero,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    if (numero < 1 || numero > mapaMisionesService.totalUnidades()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    VistaUnidad vista = leccionService.obtenerUnidad(usuario.getId(), numero);
    if (vista.unidad().isBloqueada()) {
      mensajes.addFlashAttribute("error", "Completa primero la unidad anterior.");
      return AL_MAPA;
    }
    model.addAttribute("unidad", vista.unidad());
    model.addAttribute("lecciones", vista.lecciones());
    model.addAttribute("energia", vista.energia());
    return "estudiante/unidad";
  }

  @GetMapping("/estudiante/lecciones/{codigo}")
  public String leccion(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    VistaLeccion vista = leccionService.obtenerLeccion(usuario.getId(), buscar(codigo));
    if (!vista.disponible()) {
      mensajes.addFlashAttribute("error", "Completa primero la lección anterior.");
      return AL_MAPA;
    }
    return mostrar(model, usuario.getId(), vista.leccion(), vista.completada(), vista.energia());
  }

  @PostMapping("/estudiante/lecciones/{codigo}")
  public String responder(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      @RequestParam(required = false) Integer respuesta,
      @RequestParam UUID intento,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    Leccion leccion = buscar(codigo);
    ResultadoLeccion resultado;
    try {
      resultado = leccionService.responder(usuario.getId(), leccion, respuesta, intento);
    } catch (MisionNoPermitidaException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      return AL_MAPA;
    }

    switch (resultado.resultado()) {
      case CORRECTA:
        model.addAttribute("acierto", mensajeDeAcierto(leccion, resultado));
        model.addAttribute("explicacion", leccion.getPregunta().explicacion());
        model.addAttribute("unidadCompletada", resultado.unidadCompletada());
        model.addAttribute("diasRacha", resultado.diasRacha());
        model.addAttribute("rachaAumentada", resultado.rachaAumentada());
        model.addAttribute("logrosNuevos", resultado.logrosNuevos());
        agregarRespuestaElegida(model, leccion, respuesta);
        break;
      case YA_COMPLETADA:
        mensajes.addFlashAttribute("mensaje", "Ya habías completado esta lección.");
        return "redirect:/estudiante/unidades/" + leccion.getUnidad();
      case SIN_ENERGIA:
        model.addAttribute("error", mensajeSinEnergia(resultado));
        break;
      case INTENTO_REPETIDO:
        model.addAttribute(
            "error", "Esta respuesta ya fue evaluada. Puedes realizar un nuevo intento.");
        break;
      case INCORRECTA:
        model.addAttribute("error", mensajeDeError(resultado));
        model.addAttribute("pista", leccion.getPregunta().pista());
        agregarRespuestaElegida(model, leccion, respuesta);
        break;
    }
    return mostrar(
        model,
        usuario.getId(),
        leccion,
        resultado.resultado() == ResultadoLeccion.Resultado.CORRECTA,
        resultado.energia());
  }

  @PostMapping("/estudiante/lecciones/{codigo}/desarrollo")
  public String entregarRespuesta(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      @RequestParam(required = false) String texto,
      Model model,
      RedirectAttributes mensajes) {
    if (!usuario.isOnboardingCompletado()) {
      return AL_MODULO_INICIAL;
    }
    Leccion leccion = buscar(codigo);
    if (leccion.getPreguntaDesarrollo() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    if (texto == null || texto.isBlank() || texto.length() > EntregaDesarrollo.MAXIMO_TEXTO) {
      model.addAttribute("textoDesarrollo", texto);
      model.addAttribute(
          "errorDesarrollo",
          "Escribe una respuesta de hasta " + EntregaDesarrollo.MAXIMO_TEXTO + " caracteres.");
      return leccion(usuario, codigo, model, mensajes);
    }
    try {
      boolean guardada = leccionService.entregarRespuesta(usuario.getId(), leccion, texto);
      mensajes.addFlashAttribute(
          "mensajeDesarrollo",
          guardada
              ? "Respuesta enviada correctamente."
              : "Ya enviaste una respuesta para esta actividad.");
      return "redirect:/estudiante/lecciones/" + leccion.getCodigo();
    } catch (MisionNoPermitidaException exception) {
      mensajes.addFlashAttribute("error", exception.getMessage());
      return AL_MAPA;
    }
  }

  private String mostrar(
      Model model, Long estudianteId, Leccion leccion, boolean completada, EstadoEnergia energia) {
    model.addAttribute("leccion", leccion);
    model.addAttribute("completada", completada);
    model.addAttribute("energia", energia);
    if (!completada && !energia.isAgotada()) {
      model.addAttribute("intento", UUID.randomUUID());
    }
    if (leccion.getPreguntaDesarrollo() != null) {
      model.addAttribute("maximoDesarrollo", EntregaDesarrollo.MAXIMO_TEXTO);
      leccionService
          .obtenerEntrega(estudianteId, leccion)
          .ifPresent(entrega -> model.addAttribute("entregaDesarrollo", entrega));
    }
    return "estudiante/leccion";
  }

  private static void agregarRespuestaElegida(Model model, Leccion leccion, Integer respuesta) {
    var opciones = leccion.getPregunta().opciones();
    if (respuesta != null && respuesta >= 0 && respuesta < opciones.size()) {
      model.addAttribute("respuestaElegida", opciones.get(respuesta));
    }
  }

  private String mensajeDeAcierto(Leccion leccion, ResultadoLeccion resultado) {
    StringBuilder mensaje =
        new StringBuilder("¡Correcto! Ganaste ").append(resultado.xpGanada()).append(" XP.");
    if (resultado.unidadCompletada()) {
      mensaje.append(" ¡Completaste la unidad ").append(leccion.getUnidad()).append("!");
      if (resultado.unidadDesbloqueada() != null) {
        mensaje
            .append(" ¡Nuevo nivel disponible! Unidad ")
            .append(resultado.unidadDesbloqueada())
            .append(": ")
            .append(mapaMisionesService.tituloDeUnidad(resultado.unidadDesbloqueada()))
            .append('.');
      } else {
        mensaje.append(" Completaste todo el mapa de aprendizaje.");
      }
    }
    return mensaje.toString();
  }

  private static String mensajeDeError(ResultadoLeccion resultado) {
    if (resultado.energia().isAgotada()) {
      return "Respuesta incorrecta. " + mensajeSinEnergia(resultado);
    }
    return "Respuesta incorrecta. Perdiste 1 de energía; te quedan "
        + resultado.energia().actual()
        + " de "
        + resultado.energia().maxima()
        + ".";
  }

  private static String mensajeSinEnergia(ResultadoLeccion resultado) {
    return "Te quedaste sin energía. Espera "
        + resultado.energia().minutosParaRecarga()
        + " min para recuperar una unidad o repasa el contenido de la lección.";
  }

  private static Leccion buscar(String codigo) {
    return Leccion.porCodigo(codigo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
