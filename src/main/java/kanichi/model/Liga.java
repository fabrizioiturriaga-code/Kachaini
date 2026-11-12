package kanichi.model;

import java.util.Arrays;
import java.util.Optional;

public enum Liga {
  AMAUTA("Liga Amauta", "🥉", 0),
  PANACA("Liga Panaca", "🥈", 40),
  AUQUI("Liga Auqui", "🥇", 80),
  INCA("Liga Inca", "🏆", 120);

  private final String nombre;
  private final String icono;
  private final int xpMinima;

  Liga(String nombre, String icono, int xpMinima) {
    this.nombre = nombre;
    this.icono = icono;
    this.xpMinima = xpMinima;
  }

  public static Liga para(int xpSemanal) {
    Liga liga = AMAUTA;
    for (Liga candidata : values()) {
      if (xpSemanal >= candidata.xpMinima) {
        liga = candidata;
      }
    }
    return liga;
  }

  public Optional<Liga> siguiente() {
    return Arrays.stream(values()).filter(l -> l.ordinal() == ordinal() + 1).findFirst();
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }

  public int getXpMinima() {
    return xpMinima;
  }
}