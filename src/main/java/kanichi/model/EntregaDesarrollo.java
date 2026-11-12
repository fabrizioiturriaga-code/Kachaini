package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "entregas_desarrollo")
public class EntregaDesarrollo {
  public static final int MAXIMO_TEXTO = 2000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(nullable = false, length = 40)
  private String leccion;

  @Column(nullable = false, length = MAXIMO_TEXTO)
  private String texto;

  @Column(name = "fecha_envio", nullable = false)
  private Instant fechaEnvio;

  protected EntregaDesarrollo() {}

  public EntregaDesarrollo(Long estudianteId, String leccion, String texto, Instant fechaEnvio) {
    this.estudianteId = estudianteId;
    this.leccion = leccion;
    this.texto = texto;
    this.fechaEnvio = fechaEnvio;
  }

  public String getTexto() {
    return texto;
  }
}
