package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import kanichi.service.MapaMisionesService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {
  private final MapaMisionesService mapaMisionesService;

  public NavegacionController(MapaMisionesService mapaMisionesService) {
    this.mapaMisionesService = mapaMisionesService;
  }

  @GetMapping("/inicio")
  public String inicio(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    var unidades = mapaMisionesService.obtenerMapa(usuario.getId());
    model.addAttribute("nombre", usuario.getNombre());
    model.addAttribute("unidades", unidades);
    model.addAttribute(
        "unidadActiva", unidades.stream().filter(u -> u.isActiva()).findFirst().orElse(null));
    return "estudiante/inicio";
  }

  @GetMapping("/estudiante/retos")
  public String retos() {
    return "estudiante/retos";
  }

  @GetMapping("/estudiante/comunidad")
  public String comunidad() {
    return "estudiante/comunidad";
  }
}