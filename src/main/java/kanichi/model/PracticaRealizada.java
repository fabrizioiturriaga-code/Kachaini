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
@Table(name = "practicas_realizadas")
public class PracticaRealizada {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoPractica tipo;

  @Column(nullable = false, length = 60)
  private String codigo;

  @Column(nullable = false)
  private int aciertos;

  @Column(nullable = false)
  private int total;

  @Column(nullable = false)
  private Instant fecha;

  protected PracticaRealizada() {}

  public PracticaRealizada(
      Long estudianteId, TipoPractica tipo, String codigo, int aciertos, int total, Instant fecha) {
    this.estudianteId = estudianteId;
    this.tipo = tipo;
    this.codigo = codigo;
    this.aciertos = aciertos;
    this.total = total;
    this.fecha = fecha;
  }

  public int getAciertos() {
    return aciertos;
  }

  public int getTotal() {
    return total;
  }
}
