package kanichi.controller;

import java.util.Arrays;
import kanichi.dto.MapaAprendizaje;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Logro;
import kanichi.service.LeccionService;
import kanichi.service.ModuloInicialService;
import kanichi.service.RankingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {
  private final LeccionService leccionService;
  private final ModuloInicialService moduloInicialService;
  private final RankingService rankingService;

  public NavegacionController(
      LeccionService leccionService,
      ModuloInicialService moduloInicialService,
      RankingService rankingService) {
    this.leccionService = leccionService;
    this.moduloInicialService = moduloInicialService;
    this.rankingService = rankingService;
  }

  /** Lecciones: mapa de aprendizaje y energía. Requiere haber completado el módulo inicial. */
  @GetMapping("/inicio")
  public String inicio(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    if (!usuario.isOnboardingCompletado()) {
      return "redirect:/onboarding";
    }
    MapaAprendizaje mapa = leccionService.obtenerMapa(usuario.getId());
    model.addAttribute("nombre", usuario.getNombre());
    model.addAttribute("unidades", mapa.unidades());
    model.addAttribute(
        "unidadActiva",
        mapa.unidades().stream().filter(UnidadMapa::isActiva).findFirst().orElse(null));
    model.addAttribute("energia", mapa.energia());
    model.addAttribute("diasRacha", mapa.diasRacha());
    return "estudiante/inicio";
  }

  /** Logros: XP acumulada e insignias obtenidas o por obtener. */
  @GetMapping("/estudiante/retos")
  public String retos(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("progreso", moduloInicialService.obtenerProgreso(usuario.getId()));
    model.addAttribute("catalogoLogros", Arrays.asList(Logro.values()));
    return "estudiante/retos";
  }

  /** Ranking del salón por XP. */
  @GetMapping("/estudiante/comunidad")
  public String comunidad(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("ranking", rankingService.obtenerRankingDelSalon(usuario.getId()));
    return "estudiante/comunidad";
  }
}
