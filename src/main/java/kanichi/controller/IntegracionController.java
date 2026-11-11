package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IntegracionController {
  @GetMapping("/docente/panel")
  public String destino(@AuthenticationPrincipal UsuarioSesion usuario, Model model) {
    model.addAttribute("usuario", usuario);
    return "integracion";
  }
}
