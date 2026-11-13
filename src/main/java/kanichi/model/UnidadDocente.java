package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "unidades_docente")
public class UnidadDocente {
  public static final int MAXIMO_TITULO = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "docente_id", nullable = false)
  private Long docenteId;

  @Column(nullable = false)
  private int grado;

  @Column(nullable = false, length = MAXIMO_TITULO)
  private String titulo;

  @Column(nullable = false)
  private Instant creada;

  @Column(nullable = false)
  private Instant actualizada;

  protected UnidadDocente() {}

  public UnidadDocente(Long docenteId, int grado, String titulo, Instant ahora) {
    this.docenteId = docenteId;
    this.grado = grado;
    this.titulo = titulo;
    this.creada = ahora;
    this.actualizada = ahora;
  }

  public void actualizar(int grado, String titulo, Instant ahora) {
    this.grado = grado;
    this.titulo = titulo;
    this.actualizada = ahora;
  }

  public void marcarActualizada(Instant ahora) {
    this.actualizada = ahora;
  }

  public boolean esDe(Long docenteId) {
    return this.docenteId.equals(docenteId);
  }

  public Long getId() {
    return id;
  }

  public Long getDocenteId() {
    return docenteId;
  }

  public int getGrado() {
    return grado;
  }

  public String getTitulo() {
    return titulo;
  }

  public Instant getActualizada() {
    return actualizada;
  }
}
