package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "retos_docente")
public class RetoDocente {

  public static final int DIAS_POR_VENCER = 2;
  public static final int LARGO_PREGUNTA = 300;
  public static final int LARGO_OPCION = 150;
  public static final int MAXIMO_OPCIONES = 4;

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public enum Estado {
    VIGENTE("Vigente"),
    POR_VENCER("Vence pronto"),
    VENCIDO("Vencido");

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

  @Column(name = "docente_id", nullable = false)
  private Long docenteId;

  @Column(nullable = false)
  private Integer grado;

  @Column(length = 1)
  private String seccion;

  @Column(nullable = false, length = 100)
  private String titulo;

  @Column(nullable = false, length = 1000)
  private String descripcion;

  @Column(nullable = false, length = 1000)
  private String objetivos;

  @Column(name = "fecha_limite", nullable = false)
  private LocalDate fechaLimite;

  @Column(nullable = false)
  private Integer xp;

  @Column(nullable = false)
  private Instant publicado;

  @Column(length = 300)
  private String pregunta;

  @Column(length = 700)
  private String opciones;

  @Column(name = "respuesta_correcta")
  private Integer respuestaCorrecta;

  protected RetoDocente() {}

  public RetoDocente(
      Long docenteId,
      int grado,
      String seccion,
      String titulo,
      String descripcion,
      String objetivos,
      LocalDate fechaLimite,
      int xp,
      Instant publicado) {
    this.docenteId = docenteId;
    this.grado = grado;
    this.seccion = seccion;
    this.titulo = titulo;
    this.descripcion = descripcion;
    this.objetivos = objetivos;
    this.fechaLimite = fechaLimite;
    this.xp = xp;
    this.publicado = publicado;
  }

  public void agregarPregunta(String pregunta, List<String> opciones, int respuestaCorrecta) {
    this.pregunta = pregunta;
    this.opciones = String.join("\n", opciones);
    this.respuestaCorrecta = respuestaCorrecta;
  }

  public boolean isConPregunta() {
    return pregunta != null;
  }

  public boolean esCorrecta(Integer respuesta) {
    return respuestaCorrecta != null && respuestaCorrecta.equals(respuesta);
  }

  public List<String> getListaOpciones() {
    return opciones == null ? List.of() : List.of(opciones.split("\n"));
  }

  public Estado estadoEn(LocalDate hoy) {
    if (hoy.isAfter(fechaLimite)) {
      return Estado.VENCIDO;
    }
    if (ChronoUnit.DAYS.between(hoy, fechaLimite) <= DIAS_POR_VENCER) {
      return Estado.POR_VENCER;
    }
    return Estado.VIGENTE;
  }

  public List<String> getListaObjetivos() {
    return Arrays.stream(objetivos.split("\\R"))
        .map(String::strip)
        .filter(objetivo -> !objetivo.isEmpty())
        .toList();
  }

  public String getDestinatarios() {
    return seccion == null ? grado + ".° (todas las secciones)" : grado + ".° " + seccion;
  }

  public String getFechaLimiteTexto() {
    return FORMATO_FECHA.format(fechaLimite);
  }

  public Long getId() {
    return id;
  }

  public Long getDocenteId() {
    return docenteId;
  }

  public Integer getGrado() {
    return grado;
  }

  public String getSeccion() {
    return seccion;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getObjetivos() {
    return objetivos;
  }

  public LocalDate getFechaLimite() {
    return fechaLimite;
  }

  public Integer getXp() {
    return xp;
  }

  public Instant getPublicado() {
    return publicado;
  }

  public String getPregunta() {
    return pregunta;
  }
}