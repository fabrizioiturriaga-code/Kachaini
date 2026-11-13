package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "multiplicadores_xp")
public class MultiplicadorXp {
  @Id
  @Column(name = "estudiante_id")
  private Long estudianteId;

  @Column(name = "docente_id", nullable = false)
  private Long docenteId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private FactorXp factor;

  @Column(nullable = false)
  private Instant inicio;

  @Column(nullable = false)
  private Instant fin;

  protected MultiplicadorXp() {}

  public MultiplicadorXp(
      Long estudianteId, Long docenteId, FactorXp factor, Instant inicio, Instant fin) {
    this.estudianteId = estudianteId;
    actualizar(docenteId, factor, inicio, fin);
  }

  public void actualizar(Long docenteId, FactorXp factor, Instant inicio, Instant fin) {
    if (!fin.isAfter(inicio)) {
      throw new IllegalArgumentException("El periodo del multiplicador no es válido.");
    }
    this.docenteId = docenteId;
    this.factor = factor;
    this.inicio = inicio;
    this.fin = fin;
  }

  public boolean estaActivoEn(Instant ahora) {
    return !ahora.isBefore(inicio) && ahora.isBefore(fin);
  }

  public Long getEstudianteId() {
    return estudianteId;
  }

  public Long getDocenteId() {
    return docenteId;
  }

  public FactorXp getFactor() {
    return factor;
  }

  public Instant getInicio() {
    return inicio;
  }

  public Instant getFin() {
    return fin;
  }
}
