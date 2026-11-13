package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UsuarioVistaAdvice {
  @ModelAttribute("usuarioActual")
  public UsuarioSesion usuarioActual(@AuthenticationPrincipal UsuarioSesion usuario) {
    return usuario;
  }
}
