package kanichi.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

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

  @Column(nullable = false)
  private int xp = 0;

  @ElementCollection
  @CollectionTable(name = "misiones_completadas", joinColumns = @JoinColumn(name = "estudiante_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "mision", nullable = false, length = 40)
  private Set<MisionInicial> misionesCompletadas = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "logros_estudiante", joinColumns = @JoinColumn(name = "estudiante_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "logro", nullable = false, length = 40)
  private Set<Logro> logros = new HashSet<>();

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

  public void completarOnboarding() {
    this.onboardingCompletado = true;
  }

  public int getXp() {
    return xp;
  }

  public boolean haCompletado(MisionInicial mision) {
    return misionesCompletadas.contains(mision);
  }

  /** Registra la misión y suma su XP. Devuelve false si ya estaba completada. */
  public boolean registrarMision(MisionInicial mision) {
    if (!misionesCompletadas.add(mision)) {
      return false;
    }
    xp += mision.getXp();
    return true;
  }

  public boolean tieneLogro(Logro logro) {
    return logros.contains(logro);
  }

  /** Otorga el logro. Devuelve false si ya lo tenía. */
  public boolean otorgarLogro(Logro logro) {
    return logros.add(logro);
  }

  public Rol getRol() {
    return Rol.ESTUDIANTE;
  }

  public boolean isAutorizado() {
    return true;
  }
}
