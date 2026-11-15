package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "reto_id", nullable = false)
  private Long retoId;

  @Column(nullable = false)
  private Instant creada;

  private Instant leida;

  private Instant completada;

  private Instant fallida;

  protected Notificacion() {}

  public Notificacion(Long estudianteId, Long retoId, Instant creada) {
    this.estudianteId = estudianteId;
    this.retoId = retoId;
    this.creada = creada;
  }

  public void marcarLeida(Instant ahora) {
    if (leida == null) {
      leida = ahora;
    }
  }

  public void completar(Instant ahora) {
    marcarLeida(ahora);
    if (completada == null) {
      completada = ahora;
    }
  }

  public void registrarFallo(Instant ahora) {
    marcarLeida(ahora);
    if (fallida == null) {
      fallida = ahora;
    }
  }

  public boolean isCompletada() {
    return completada != null;
  }

  public boolean isFallida() {
    return fallida != null;
  }

  public boolean isLeida() {
    return leida != null;
  }

  public Long getId() {
    return id;
  }

  public Long getEstudianteId() {
    return estudianteId;
  }

  public Long getRetoId() {
    return retoId;
  }

  public Instant getCreada() {
    return creada;
  }

  public Instant getLeida() {
    return leida;
  }

  public Instant getCompletada() {
    return completada;
  }

  public Instant getFallida() {
    return fallida;
  }
}