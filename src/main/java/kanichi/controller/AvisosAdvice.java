package kanichi.controller;

import kanichi.dto.ResumenAvisos;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Rol;
import kanichi.service.RetoDocenteService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AvisosAdvice {
  private final ObjectProvider<RetoDocenteService> retoDocenteService;

  public AvisosAdvice(ObjectProvider<RetoDocenteService> retoDocenteService) {
    this.retoDocenteService = retoDocenteService;
  }

  @ModelAttribute("resumenAvisos")
  public ResumenAvisos resumenAvisos(@AuthenticationPrincipal UsuarioSesion usuario) {
    if (usuario == null || usuario.getRol() != Rol.ESTUDIANTE) {
      return ResumenAvisos.VACIO;
    }
    RetoDocenteService servicio = retoDocenteService.getIfAvailable();
    if (servicio == null) {
      return ResumenAvisos.VACIO;
    }
    ResumenAvisos resumen = servicio.resumen(usuario.getId());
    return resumen == null ? ResumenAvisos.VACIO : resumen;
  }
}