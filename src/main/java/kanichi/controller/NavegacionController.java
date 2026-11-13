package kanichi.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kanichi.dto.MapaAprendizaje;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Liga;
import kanichi.model.Logro;
import kanichi.service.LeccionDocenteService;
import kanichi.service.LeccionService;
import kanichi.service.LigaService;
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
  private final LigaService ligaService;
  private final LeccionDocenteService leccionDocenteService;

  public NavegacionController(
      LeccionService leccionService,
      ModuloInicialService moduloInicialService,
      RankingService rankingService,
      LigaService ligaService,
      LeccionDocenteService leccionDocenteService) {
    this.leccionService = leccionService;
    this.moduloInicialService = moduloInicialService;
    this.rankingService = rankingService;
    this.ligaService = ligaService;
    this.leccionDocenteService = leccionDocenteService;
  }

  @GetMapping("/inicio")
  public String inicio(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    if (!usuario.isOnboardingCompletado()) {
      return "redirect:/onboarding";
    }
    model.addAttribute("notificaciones", ligaService.actualizarYNotificar(usuario.getId()));
    MapaAprendizaje mapa = leccionService.obtenerMapa(usuario.getId());
    model.addAttribute("nombre", usuario.getNombre());
    model.addAttribute("unidades", mapa.unidades());
    model.addAttribute(
        "unidadesDocente", leccionDocenteService.unidadesDelMapa(usuario.getId()));
    model.addAttribute(
        "unidadActiva",
        mapa.unidades().stream().filter(UnidadMapa::isActiva).findFirst().orElse(null));
    model.addAttribute("energia", mapa.energia());
    model.addAttribute("diasRacha", mapa.diasRacha());
    return "estudiante/inicio";
  }

  @GetMapping("/estudiante/retos")
  public String retos(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("progreso", moduloInicialService.obtenerProgreso(usuario.getId()));
    model.addAttribute("catalogoLogros", Arrays.asList(Logro.values()));
    return "estudiante/retos";
  }

  @GetMapping("/estudiante/comunidad")
  public String comunidad(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute(
        "notificaciones", ligaService.actualizarSalonYNotificar(usuario.getId()));
    model.addAttribute("ranking", rankingService.obtenerRankingDelSalon(usuario.getId()));
    List<Liga> ligas = new ArrayList<>(Arrays.asList(Liga.values()));
    Collections.reverse(ligas);
    model.addAttribute("ligas", ligas);
    return "estudiante/comunidad";
  }
}