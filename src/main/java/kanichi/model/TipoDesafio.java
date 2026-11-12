package kanichi.model;

public enum TipoDesafio {
  DUO_EN_MARCHA("Dúo en marcha", "🤝", 4, 80, 30),
  EQUIPO_EXPERTO("Equipo experto", "🧠", 8, 90, 60);

  private final String nombre;
  private final String icono;
  private final int metaTareas;
  private final int precisionMinima;
  private final int xpRecompensa;

  TipoDesafio(
      String nombre, String icono, int metaTareas, int precisionMinima, int xpRecompensa) {
    this.nombre = nombre;
    this.icono = icono;
    this.metaTareas = metaTareas;
    this.precisionMinima = precisionMinima;
    this.xpRecompensa = xpRecompensa;
  }

  public String getDescripcion() {
    return "Completen "
        + metaTareas
        + " tareas con al menos "
        + precisionMinima
        + " % de aciertos. Recompensa: "
        + xpRecompensa
        + " XP para cada uno.";
  }

  public boolean cumplido(long tareas, long aciertos, long intentos) {
    return tareas >= metaTareas && intentos > 0 && aciertos * 100 >= precisionMinima * intentos;
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }

  public int getMetaTareas() {
    return metaTareas;
  }

  public int getPrecisionMinima() {
    return precisionMinima;
  }

  public int getXpRecompensa() {
    return xpRecompensa;
  }
}
