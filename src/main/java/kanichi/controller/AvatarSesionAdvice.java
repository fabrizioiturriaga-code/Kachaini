package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = NavegacionController.class)
public class AvatarSesionAdvice {
  @ModelAttribute("avatarSesion")
  public String avatarSesion(@AuthenticationPrincipal UsuarioSesion usuario) {
    return usuario == null ? null : usuario.getAvatar();
  }
}
