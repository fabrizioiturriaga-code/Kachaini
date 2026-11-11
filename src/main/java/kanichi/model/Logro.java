package kanichi.model;

/** Insignias simbólicas que el estudiante obtiene al avanzar. */
public enum Logro {
  PRIMEROS_PASOS("Primeros pasos", "👣", "Completaste el tutorial de Kaichaini."),
  PRIMER_ACIERTO("Primer acierto", "🎯", "Respondiste correctamente tu primer reto."),
  EXPLORADOR("Explorador de Kaichaini", "🧭", "Completaste el módulo inicial.");

  private final String nombre;
  private final String icono;
  private final String descripcion;

  Logro(String nombre, String icono, String descripcion) {
    this.nombre = nombre;
    this.icono = icono;
    this.descripcion = descripcion;
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }

  public String getDescripcion() {
    return descripcion;
  }
}
