package kanichi.controller;

import kanichi.dto.UsuarioSesion;
import kanichi.model.Rol;
import kanichi.service.MensajeriaService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MensajesAdvice {
  private final ObjectProvider<MensajeriaService> mensajeriaService;

  public MensajesAdvice(ObjectProvider<MensajeriaService> mensajeriaService) {
    this.mensajeriaService = mensajeriaService;
  }

  @ModelAttribute("mensajesSinLeer")
  public int mensajesSinLeer(@AuthenticationPrincipal UsuarioSesion usuario) {
    if (usuario == null) {
      return 0;
    }
    MensajeriaService servicio = mensajeriaService.getIfAvailable();
    if (servicio == null) {
      return 0;
    }
    return usuario.getRol() == Rol.DOCENTE
        ? servicio.sinLeerDocente(usuario.getId())
        : servicio.sinLeerEstudiante(usuario.getId());
  }
}
