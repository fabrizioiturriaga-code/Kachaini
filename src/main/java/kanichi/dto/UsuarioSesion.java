package kanichi.dto;

import java.util.List;
import kanichi.model.Rol;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UsuarioSesion extends User {
  private final Long id;
  private String nombre;
  private final Rol rol;
  private boolean onboardingCompletado;
  private String avatar;

  public UsuarioSesion(
      Long id,
      String nombre,
      String correo,
      String contrasenaHash,
      Rol rol,
      boolean autorizado,
      boolean onboardingCompletado) {
    super(
        correo,
        contrasenaHash,
        autorizado,
        true,
        true,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    this.id = id;
    this.nombre = nombre;
    this.rol = rol;
    this.onboardingCompletado = onboardingCompletado;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public void actualizarNombreVisible(String nombre) {
    this.nombre = nombre;
  }

  public String getAvatar() {
    return avatar;
  }

  public void actualizarAvatar(String avatar) {
    this.avatar = avatar;
  }

  public Rol getRol() {
    return rol;
  }

  public boolean isOnboardingCompletado() {
    return onboardingCompletado;
  }

  /** Actualiza la identidad de la sesión cuando el estudiante termina el módulo inicial. */
  public void marcarOnboardingCompletado() {
    this.onboardingCompletado = true;
  }

  public String destino() {
    if (rol == Rol.DOCENTE) {
      return "/docente/panel";
    }
    if (onboardingCompletado) {
      return "/inicio";
    }
    return "/onboarding";
  }
}
