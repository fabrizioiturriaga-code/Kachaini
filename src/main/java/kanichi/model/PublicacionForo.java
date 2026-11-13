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
@Table(name = "publicaciones_foro")
public class PublicacionForo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer grado;

  @Column(nullable = false, length = 1)
  private String seccion;

  @Column(name = "autor_id")
  private Long autorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoPublicacion tipo;

  @Column(nullable = false, length = 500)
  private String texto;

  @Column(length = 100, unique = true)
  private String clave;

  @Column(nullable = false)
  private Instant creada;

  protected PublicacionForo() {}

  public PublicacionForo(
      Integer grado,
      String seccion,
      Long autorId,
      TipoPublicacion tipo,
      String texto,
      String clave,
      Instant creada) {
    this.grado = grado;
    this.seccion = seccion;
    this.autorId = autorId;
    this.tipo = tipo;
    this.texto = texto;
    this.clave = clave;
    this.creada = creada;
  }

  public static PublicacionForo mensaje(Estudiante autor, String texto, Instant creada) {
    return new PublicacionForo(
        autor.getGrado(),
        autor.getSeccion(),
        autor.getId(),
        TipoPublicacion.MENSAJE,
        texto,
        null,
        creada);
  }

  public Long getId() {
    return id;
  }

  public Integer getGrado() {
    return grado;
  }

  public String getSeccion() {
    return seccion;
  }

  public Long getAutorId() {
    return autorId;
  }

  public TipoPublicacion getTipo() {
    return tipo;
  }

  public String getTexto() {
    return texto;
  }

  public String getClave() {
    return clave;
  }

  public Instant getCreada() {
    return creada;
  }
}