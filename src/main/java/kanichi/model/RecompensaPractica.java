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
@Table(name = "recompensas_practica")
public class RecompensaPractica {
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
  private int xp;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private Coleccionable coleccionable;

  @Column(nullable = false)
  private Instant fecha;

  protected RecompensaPractica() {}

  public RecompensaPractica(
      Long estudianteId,
      TipoPractica tipo,
      String codigo,
      int xp,
      Coleccionable coleccionable,
      Instant fecha) {
    this.estudianteId = estudianteId;
    this.tipo = tipo;
    this.codigo = codigo;
    this.xp = xp;
    this.coleccionable = coleccionable;
    this.fecha = fecha;
  }

  public Coleccionable getColeccionable() {
    return coleccionable;
  }
}
