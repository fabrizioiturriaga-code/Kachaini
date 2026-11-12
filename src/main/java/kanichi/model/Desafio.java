package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "desafios")
public class Desafio {
  public enum Estado {
    PENDIENTE("Pendiente"),
    ACTIVO("Activo"),
    COMPLETADO("Completado"),
    RECHAZADO("Rechazado");

    private final String etiqueta;

    Estado(String etiqueta) {
      this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
      return etiqueta;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "creador_id", nullable = false)
  private Long creadorId;

  @Column(name = "companero_id", nullable = false)
  private Long companeroId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoDesafio tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Estado estado = Estado.PENDIENTE;

  @Column(nullable = false)
  private Instant creado;

  private Instant iniciado;

  private Instant completado;

  protected Desafio() {}

  public Desafio(Long creadorId, Long companeroId, TipoDesafio tipo, Instant creado) {
    this.creadorId = creadorId;
    this.companeroId = companeroId;
    this.tipo = tipo;
    this.creado = creado;
  }

  public boolean participa(Long estudianteId) {
    return creadorId.equals(estudianteId) || companeroId.equals(estudianteId);
  }

  public void aceptar(Instant ahora) {
    estado = Estado.ACTIVO;
    iniciado = ahora;
  }

  public void rechazar() {
    estado = Estado.RECHAZADO;
  }

  public void completar(Instant ahora) {
    estado = Estado.COMPLETADO;
    completado = ahora;
  }

  public Long getId() {
    return id;
  }

  public Long getCreadorId() {
    return creadorId;
  }

  public Long getCompaneroId() {
    return companeroId;
  }

  public TipoDesafio getTipo() {
    return tipo;
  }

  public Estado getEstado() {
    return estado;
  }

  public Instant getIniciado() {
    return iniciado;
  }

  public Instant getCompletado() {
    return completado;
  }
}
