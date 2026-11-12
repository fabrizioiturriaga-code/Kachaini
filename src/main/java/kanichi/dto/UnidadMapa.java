package kanichi.dto;

import kanichi.model.Leccion;

/** Unidad del mapa de aprendizaje con su estado y el avance de sus lecciones (HU-18). */
public record UnidadMapa(
    int numero, String titulo, Estado estado, int leccionesCompletadas, int leccionesTotal) {
  public enum Estado {
    COMPLETADA,
    ACTIVA,
    BLOQUEADA
  }

  public UnidadMapa(int numero, String titulo, Estado estado) {
    this(numero, titulo, estado, 0, Leccion.LECCIONES_POR_UNIDAD);
  }

  public boolean isActiva() {
    return estado == Estado.ACTIVA;
  }

  public boolean isBloqueada() {
    return estado == Estado.BLOQUEADA;
  }

  public boolean isCompletada() {
    return estado == Estado.COMPLETADA;
  }

  public String claseCss() {
    return "unidad--" + estado.name().toLowerCase();
  }

  /** Avance de la unidad, de 0 a 100. */
  public int porcentaje() {
    return leccionesTotal == 0 ? 0 : leccionesCompletadas * 100 / leccionesTotal;
  }

  /** Estrellas de avance: una por lección completada. */
  public String estrellas() {
    return "★".repeat(leccionesCompletadas) + "☆".repeat(leccionesTotal - leccionesCompletadas);
  }
}
