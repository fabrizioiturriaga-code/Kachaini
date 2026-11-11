package kanichi.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Misiones introductorias del módulo inicial, en el orden en que deben completarse. Completar
 * todas desbloquea la primera unidad del mapa de misiones.
 */
public enum MisionInicial {
  BIENVENIDA(
      "bienvenida",
      Tipo.TUTORIAL,
      "Bienvenida a Kaichaini",
      List.of(
          "Kaichaini es tu espacio para aprender la historia del Perú jugando.",
          "En el mapa de misiones de Inicio encontrarás las unidades del curso. Cada unidad se"
              + " desbloquea cuando completas la anterior.",
          "Al completar misiones ganas XP (puntos de experiencia) e insignias. Puedes verlas en"
              + " Retos.",
          "En Comunidad verás el ranking de tu salón según la XP acumulada.",
          "Usa la barra inferior para moverte entre Inicio, Retos, Comunidad y Perfil."),
      10,
      Logro.PRIMEROS_PASOS,
      null),
  QUE_ES_LA_HISTORIA(
      "que-es-la-historia",
      Tipo.LECCION,
      "¿Qué es la historia?",
      List.of(
          "La historia estudia los hechos del pasado de la humanidad para comprender el presente.",
          "Para conocer el pasado se usan fuentes. Las fuentes primarias se produjeron en la época"
              + " estudiada, como cartas, objetos o restos arqueológicos. Las fuentes secundarias"
              + " son interpretaciones posteriores, como libros o documentales.",
          "Los restos de Caral, las crónicas coloniales y los periódicos de la Independencia son"
              + " ejemplos de fuentes que nos permiten conocer el pasado del Perú."),
      20,
      null,
      null),
  LINEA_DE_TIEMPO(
      "linea-de-tiempo",
      Tipo.LECCION,
      "Ubicarnos en el tiempo",
      List.of(
          "Para ordenar los hechos históricos usamos la línea de tiempo.",
          "Los años anteriores al nacimiento de Cristo se escriben con a. C. y los posteriores con"
              + " d. C. Un siglo es un periodo de 100 años: el siglo XVI va de 1501 a 1600.",
          "Por ejemplo, Caral floreció hacia el 3000 a. C., el Tahuantinsuyo se expandió en el"
              + " siglo XV y la Independencia del Perú se proclamó en 1821."),
      20,
      null,
      null),
  RETO_DE_BIENVENIDA(
      "reto-de-bienvenida",
      Tipo.PREGUNTA,
      "Reto de bienvenida",
      List.of("Pon a prueba lo que aprendiste en las lecciones anteriores."),
      30,
      Logro.PRIMER_ACIERTO,
      new Pregunta(
          "¿Cuál de estas es una fuente primaria?",
          List.of(
              "Un libro escolar de historia publicado este año",
              "Una carta escrita por un soldado durante la Independencia",
              "Un documental sobre los incas"),
          1));

  public enum Tipo {
    TUTORIAL("Tutorial"),
    LECCION("Lección"),
    PREGUNTA("Reto");

    private final String etiqueta;

    Tipo(String etiqueta) {
      this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
      return etiqueta;
    }
  }

  public record Pregunta(String enunciado, List<String> opciones, int respuestaCorrecta) {}

  private final String codigo;
  private final Tipo tipo;
  private final String titulo;
  private final List<String> contenido;
  private final int xp;
  private final Logro insignia;
  private final Pregunta pregunta;

  MisionInicial(
      String codigo,
      Tipo tipo,
      String titulo,
      List<String> contenido,
      int xp,
      Logro insignia,
      Pregunta pregunta) {
    this.codigo = codigo;
    this.tipo = tipo;
    this.titulo = titulo;
    this.contenido = contenido;
    this.xp = xp;
    this.insignia = insignia;
    this.pregunta = pregunta;
  }

  public static Optional<MisionInicial> porCodigo(String codigo) {
    return Arrays.stream(values()).filter(m -> m.codigo.equals(codigo)).findFirst();
  }

  public static int xpTotal() {
    return Arrays.stream(values()).mapToInt(MisionInicial::getXp).sum();
  }

  /** Misión que debe completarse antes que esta, si existe. */
  public Optional<MisionInicial> anterior() {
    return ordinal() == 0 ? Optional.empty() : Optional.of(values()[ordinal() - 1]);
  }

  public boolean aceptaRespuesta(Integer respuesta) {
    return pregunta == null
        || (respuesta != null && respuesta == pregunta.respuestaCorrecta());
  }

  public int getNumero() {
    return ordinal() + 1;
  }

  public String getCodigo() {
    return codigo;
  }

  public Tipo getTipo() {
    return tipo;
  }

  public String getTitulo() {
    return titulo;
  }

  public List<String> getContenido() {
    return contenido;
  }

  public int getXp() {
    return xp;
  }

  public Optional<Logro> getInsignia() {
    return Optional.ofNullable(insignia);
  }

  public Pregunta getPregunta() {
    return pregunta;
  }
}
