package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mensajes_directos")
public class MensajeDirecto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "docente_id", nullable = false)
  private Long docenteId;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "enviado_por_docente", nullable = false)
  private boolean enviadoPorDocente;

  @Column(nullable = false, length = 1000)
  private String texto;

  @Column(nullable = false)
  private Instant enviado;

  private Instant leido;

  protected MensajeDirecto() {}

  private MensajeDirecto(
      Long docenteId, Long estudianteId, boolean enviadoPorDocente, String texto, Instant enviado) {
    this.docenteId = docenteId;
    this.estudianteId = estudianteId;
    this.enviadoPorDocente = enviadoPorDocente;
    this.texto = texto;
    this.enviado = enviado;
  }

  public static MensajeDirecto deDocente(
      Long docenteId, Long estudianteId, String texto, Instant enviado) {
    return new MensajeDirecto(docenteId, estudianteId, true, texto, enviado);
  }

  public static MensajeDirecto deEstudiante(
      Long estudianteId, Long docenteId, String texto, Instant enviado) {
    return new MensajeDirecto(docenteId, estudianteId, false, texto, enviado);
  }

  public Long getId() {
    return id;
  }

  public Long getDocenteId() {
    return docenteId;
  }

  public Long getEstudianteId() {
    return estudianteId;
  }

  public boolean isEnviadoPorDocente() {
    return enviadoPorDocente;
  }

  public String getTexto() {
    return texto;
  }

  public Instant getEnviado() {
    return enviado;
  }

  public Instant getLeido() {
    return leido;
  }

  public boolean isLeido() {
    return leido != null;
  }
}
