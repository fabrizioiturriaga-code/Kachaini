package kanichi.dto;

import java.util.List;

public record ForoSalon(int grado, String seccion, List<MensajeForo> mensajes) {
  public String salon() {
    return grado + ".° " + seccion;
  }
}