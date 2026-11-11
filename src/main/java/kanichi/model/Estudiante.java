package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "estudiantes")
public class Estudiante extends Usuario {
  @Column(nullable = false)
  private Integer grado;

  @Column(nullable = false, length = 1)
  private String seccion;

  @Column(name = "onboarding_completado", nullable = false)
  private boolean onboardingCompletado = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AvatarEstudiante avatar = AvatarEstudiante.KAI;

  protected Estudiante() {}

  public Estudiante(
      String nombre, String correo, String contrasenaHash, Integer grado, String seccion) {
    super(nombre, correo, contrasenaHash);
    this.grado = grado;
    this.seccion = seccion;
  }

  public Integer getGrado() {
    return grado;
  }

  public String getSeccion() {
    return seccion;
  }

  public boolean isOnboardingCompletado() {
    return onboardingCompletado;
  }

  public AvatarEstudiante getAvatar() {
    return avatar;
  }

  public void actualizarAvatar(AvatarEstudiante avatar) {
    this.avatar = avatar;
  }

  public Rol getRol() {
    return Rol.ESTUDIANTE;
  }

  public boolean isAutorizado() {
    return true;
  }
}
