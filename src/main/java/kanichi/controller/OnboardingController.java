package kanichi.controller;

import java.util.stream.Collectors;
import kanichi.dto.ProgresoModulo;
import kanichi.dto.ProgresoModulo.EstadoMision;
import kanichi.dto.ResultadoMision;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.MisionInicial;
import kanichi.service.MapaMisionesService;
import kanichi.service.ModuloInicialService;
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

/** HU-06: módulo inicial con tutorial, lecciones básicas y misiones introductorias. */
@Controller
public class OnboardingController {
  private final ModuloInicialService moduloInicialService;
  private final MapaMisionesService mapaMisionesService;

  public OnboardingController(
      ModuloInicialService moduloInicialService, MapaMisionesService mapaMisionesService) {
    this.moduloInicialService = moduloInicialService;
    this.mapaMisionesService = mapaMisionesService;
  }

  @GetMapping("/onboarding")
  public String modulo(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("nombre", usuario.getNombre());
    model.addAttribute("progreso", moduloInicialService.obtenerProgreso(usuario.getId()));
    model.addAttribute("xpTotal", MisionInicial.xpTotal());
    return "estudiante/onboarding";
  }

  @GetMapping("/onboarding/misiones/{codigo}")
  public String mision(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      Model model,
      RedirectAttributes mensajes) {
    MisionInicial mision = buscar(codigo);
    EstadoMision estado = estadoDe(moduloInicialService.obtenerProgreso(usuario.getId()), mision);
    if (!estado.disponible()) {
      mensajes.addFlashAttribute("error", "Completa primero la misión anterior.");
      return "redirect:/onboarding";
    }
    model.addAttribute("mision", mision);
    model.addAttribute("completada", estado.completada());
    return "estudiante/mision";
  }

  @PostMapping("/onboarding/misiones/{codigo}")
  public String completar(
      @AuthenticationPrincipal UsuarioSesion usuario,
      @PathVariable String codigo,
      @RequestParam(required = false) Integer respuesta,
      Model model,
      RedirectAttributes mensajes) {
    MisionInicial mision = buscar(codigo);
    ResultadoMision resultado;
    try {
      resultado = moduloInicialService.completar(usuario.getId(), mision, respuesta);
    } catch (MisionNoPermitidaException exception) {
      model.addAttribute("mision", mision);
      model.addAttribute("completada", false);
      model.addAttribute("error", exception.getMessage());
      return "estudiante/mision";
    }

    if (resultado.moduloCompletado()) {
      usuario.marcarOnboardingCompletado();
    }
    mensajes.addFlashAttribute("mensaje", mensajeDeRecompensa(resultado));
    return resultado.moduloCompletado() ? "redirect:/inicio" : "redirect:/onboarding";
  }

  private String mensajeDeRecompensa(ResultadoMision resultado) {
    if (resultado.xpGanada() == 0) {
      return "Ya habías completado esta misión.";
    }
    StringBuilder mensaje =
        new StringBuilder("¡Misión completada! Ganaste ").append(resultado.xpGanada()).append(" XP");
    if (!resultado.logrosNuevos().isEmpty()) {
      mensaje
          .append(" y la insignia ")
          .append(
              resultado.logrosNuevos().stream()
                  .map(l -> l.getIcono() + " " + l.getNombre())
                  .collect(Collectors.joining(", ")));
    }
    mensaje.append('.');
    if (resultado.moduloCompletado()) {
      mensaje
          .append(" Completaste el módulo inicial y se desbloqueó la unidad 1: ")
          .append(mapaMisionesService.primeraUnidad())
          .append('.');
    }
    return mensaje.toString();
  }

  private static MisionInicial buscar(String codigo) {
    return MisionInicial.porCodigo(codigo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static EstadoMision estadoDe(ProgresoModulo progreso, MisionInicial mision) {
    return progreso.misiones().stream()
        .filter(e -> e.mision() == mision)
        .findFirst()
        .orElseThrow();
  }
}
