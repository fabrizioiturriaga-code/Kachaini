package kanichi.model;

public enum AvatarEstudiante {
  KAI("Kai", "✨"),
  ZORRO("Zorro", "🦊"),
  COLIBRI("Colibrí", "🐦"),
  PUMA("Puma", "🐆"),
  CONDOR("Cóndor", "🦅");

  private final String nombre;
  private final String icono;

  AvatarEstudiante(String nombre, String icono) {
    this.nombre = nombre;
    this.icono = icono;
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }
}
