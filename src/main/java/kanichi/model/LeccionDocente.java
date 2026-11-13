package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "lecciones_docente")
public class LeccionDocente {
  public static final int XP = 20;
  public static final int MAXIMO_TITULO = 100;
  public static final int MAXIMO_CONTENIDO = 3000;
  public static final int MAXIMO_PREGUNTA = 300;
  public static final int MAXIMO_OPCION = 200;
  public static final int OPCIONES = 3;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "unidad_id", nullable = false)
  private Long unidadId;

  @Column(nullable = false)
  private int orden;

  @Column(nullable = false, length = MAXIMO_TITULO)
  private String titulo;

  @Column(nullable = false, length = MAXIMO_CONTENIDO)
  private String contenido;

  @Column(nullable = false, length = MAXIMO_PREGUNTA)
  private String pregunta;

  @Column(name = "opcion_a", nullable = false, length = MAXIMO_OPCION)
  private String opcionA;

  @Column(name = "opcion_b", nullable = false, length = MAXIMO_OPCION)
  private String opcionB;

  @Column(name = "opcion_c", nullable = false, length = MAXIMO_OPCION)
  private String opcionC;

  @Column(name = "respuesta_correcta", nullable = false)
  private int respuestaCorrecta;

  protected LeccionDocente() {}

  public LeccionDocente(Long unidadId, int orden, Datos datos) {
    this.unidadId = unidadId;
    this.orden = orden;
    actualizar(datos);
  }

  public void actualizar(Datos datos) {
    this.titulo = datos.titulo();
    this.contenido = datos.contenido();
    this.pregunta = datos.pregunta();
    this.opcionA = datos.opciones().get(0);
    this.opcionB = datos.opciones().get(1);
    this.opcionC = datos.opciones().get(2);
    this.respuestaCorrecta = datos.respuestaCorrecta();
  }

  public void moverA(int orden) {
    this.orden = orden;
  }

  public boolean aceptaRespuesta(Integer respuesta) {
    return respuesta != null && respuesta == respuestaCorrecta;
  }

  public List<String> getParrafos() {
    return Arrays.stream(contenido.split("\\R"))
        .map(String::strip)
        .filter(parrafo -> !parrafo.isEmpty())
        .toList();
  }

  public List<String> getOpciones() {
    return List.of(opcionA, opcionB, opcionC);
  }

  public int getXp() {
    return XP;
  }

  public Long getId() {
    return id;
  }

  public Long getUnidadId() {
    return unidadId;
  }

  public int getOrden() {
    return orden;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getContenido() {
    return contenido;
  }

  public String getPregunta() {
    return pregunta;
  }

  public int getRespuestaCorrecta() {
    return respuestaCorrecta;
  }

  public record Datos(
      String titulo,
      String contenido,
      String pregunta,
      List<String> opciones,
      int respuestaCorrecta) {}
}
