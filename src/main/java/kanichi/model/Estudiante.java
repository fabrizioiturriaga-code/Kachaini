package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  public Rol getRol() {
    return Rol.ESTUDIANTE;
  }

  public boolean isAutorizado() {
    return true;
  }
}
