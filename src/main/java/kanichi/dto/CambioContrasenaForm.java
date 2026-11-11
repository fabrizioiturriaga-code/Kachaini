package kanichi.dto;

import jakarta.validation.constraints.NotBlank;

public class CambioContrasenaForm {
  @NotBlank private String contrasenaActual;

  @NotBlank private String contrasenaNueva;

  @NotBlank private String confirmacion;

  public String getContrasenaActual() {
    return contrasenaActual;
  }

  public void setContrasenaActual(String contrasenaActual) {
    this.contrasenaActual = contrasenaActual;
  }

  public String getContrasenaNueva() {
    return contrasenaNueva;
  }

  public void setContrasenaNueva(String contrasenaNueva) {
    this.contrasenaNueva = contrasenaNueva;
  }

  public String getConfirmacion() {
    return confirmacion;
  }

  public void setConfirmacion(String confirmacion) {
    this.confirmacion = confirmacion;
  }
}
