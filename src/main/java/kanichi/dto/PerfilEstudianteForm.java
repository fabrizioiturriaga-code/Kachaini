package kanichi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilEstudianteForm {
  @NotBlank
  @Size(max = 100)
  private String nombre;

  @NotBlank
  @Pattern(regexp = "KAI|ZORRO|COLIBRI|PUMA|CONDOR")
  private String avatar;

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }
}
