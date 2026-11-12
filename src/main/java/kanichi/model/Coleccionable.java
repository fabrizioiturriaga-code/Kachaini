package kanichi.model;

import java.util.Arrays;
import java.util.Optional;

public enum Coleccionable {
  PUNTA_DE_PROYECTIL(1, "Punta de proyectil", "🪨"),
  FLAUTA_DE_CARAL(2, "Flauta de Caral", "🎶"),
  QUIPU(3, "Quipu", "🧶"),
  YELMO_ESPANOL(4, "Yelmo español", "⛑️"),
  MONEDA_VIRREINAL(5, "Moneda virreinal", "🪙"),
  ACTA_DE_LA_INDEPENDENCIA(6, "Acta de la Independencia", "📜"),
  ISLA_GUANERA(7, "Isla guanera", "🏝️"),
  ANCLA_DEL_HUASCAR(8, "Ancla del Huáscar", "⚓"),
  RADIO_ANTIGUA(9, "Radio antigua", "📻"),
  CONSTITUCION(10, "Constitución del Perú", "📘");

  private final int unidad;
  private final String nombre;
  private final String icono;

  Coleccionable(int unidad, String nombre, String icono) {
    this.unidad = unidad;
    this.nombre = nombre;
    this.icono = icono;
  }

  public static Optional<Coleccionable> deUnidad(int unidad) {
    return Arrays.stream(values()).filter(c -> c.unidad == unidad).findFirst();
  }

  public int getUnidad() {
    return unidad;
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }
}
