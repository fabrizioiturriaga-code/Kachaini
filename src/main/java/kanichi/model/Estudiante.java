package kanichi.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kanichi.dto.EstadoEnergia;

@Entity
@Table(name = "estudiantes")
public class Estudiante extends Usuario {
  /** HU-12: capacidad de la barra de energía. */
  public static final int ENERGIA_MAXIMA = 5;

  /** HU-12: tiempo que tarda en recuperarse una unidad de energía. */
  public static final Duration RECARGA_POR_UNIDAD = Duration.ofMinutes(10);

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

  @ElementCollection
  @CollectionTable(
      name = "lecciones_completadas",
      joinColumns = @JoinColumn(name = "estudiante_id"))
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "leccion", nullable = false, length = 40)
  @Column(name = "fecha", nullable = false)
  private Map<Leccion, Instant> leccionesCompletadas = new HashMap<>();

  @Column(nullable = false)
  private int energia = ENERGIA_MAXIMA;

  @Column(name = "energia_actualizada", nullable = false)
  private Instant energiaActualizada = Instant.now();

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

  public int cantidadMisionesCompletadas() {
    return misionesCompletadas.size();
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

  public boolean haCompletado(Leccion leccion) {
    return leccionesCompletadas.containsKey(leccion);
  }

  public Set<Leccion> getLeccionesCompletadas() {
    return Set.copyOf(leccionesCompletadas.keySet());
  }

  /** Registra la lección y suma su XP. Devuelve false si ya estaba completada. */
  public boolean registrarLeccion(Leccion leccion, Instant fecha) {
    if (leccionesCompletadas.putIfAbsent(leccion, fecha) != null) {
      return false;
    }
    xp += leccion.getXp();
    return true;
  }

  public RachaDiaria rachaEn(Instant ahora) {
    return RachaDiaria.calcular(leccionesCompletadas.values(), ahora);
  }

  /** Energía que tendría el estudiante en {@code ahora}, sin modificar su estado. */
  public EstadoEnergia estadoEnergia(Instant ahora) {
    int actual = (int) Math.min(ENERGIA_MAXIMA, (long) energia + ciclosDeRecarga(ahora));
    if (actual >= ENERGIA_MAXIMA) {
      return new EstadoEnergia(ENERGIA_MAXIMA, ENERGIA_MAXIMA, 0);
    }
    Duration transcurrido = transcurridoDesdeActualizacion(ahora);
    Duration enCiclo =
        transcurrido.minus(
            RECARGA_POR_UNIDAD.multipliedBy(transcurrido.dividedBy(RECARGA_POR_UNIDAD)));
    return new EstadoEnergia(
        actual, ENERGIA_MAXIMA, RECARGA_POR_UNIDAD.minus(enCiclo).getSeconds());
  }

  /** Aplica al estado guardado la energía recuperada con el paso del tiempo. */
  public void recargarEnergia(Instant ahora) {
    if (energia >= ENERGIA_MAXIMA) {
      energiaActualizada = ahora;
      return;
    }
    long ciclos = ciclosDeRecarga(ahora);
    if (ciclos == 0) {
      return;
    }
    energia = (int) Math.min(ENERGIA_MAXIMA, (long) energia + ciclos);
    energiaActualizada =
        energia >= ENERGIA_MAXIMA
            ? ahora
            : energiaActualizada.plus(RECARGA_POR_UNIDAD.multipliedBy(ciclos));
  }

  /** Resta una unidad de energía por una respuesta errónea. Devuelve false si ya no tenía. */
  public boolean perderEnergia(Instant ahora) {
    recargarEnergia(ahora);
    if (energia <= 0) {
      return false;
    }
    if (energia == ENERGIA_MAXIMA) {
      energiaActualizada = ahora;
    }
    energia--;
    return true;
  }

  private long ciclosDeRecarga(Instant ahora) {
    return energia >= ENERGIA_MAXIMA
        ? 0
        : transcurridoDesdeActualizacion(ahora).dividedBy(RECARGA_POR_UNIDAD);
  }

  private Duration transcurridoDesdeActualizacion(Instant ahora) {
    Duration transcurrido = Duration.between(energiaActualizada, ahora);
    return transcurrido.isNegative() ? Duration.ZERO : transcurrido;
  }

  public Rol getRol() {
    return Rol.ESTUDIANTE;
  }

  public boolean isAutorizado() {
    return true;
  }
}
