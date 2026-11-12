package kanichi.model;

public enum TipoPractica {
  LECTURA("Lectura"),
  ARRASTRE("Arrastrar y soltar");

  private final String nombre;

  TipoPractica(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }
}
