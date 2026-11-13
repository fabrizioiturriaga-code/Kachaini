package kanichi.dto;

import java.util.List;

public record Conversacion(Contacto contacto, List<MensajeChat> mensajes) {

  public record Contacto(Long id, String nombre, String avatar, String detalle) {}

  public record MensajeChat(String texto, String fecha, boolean propio, boolean leido) {}

  public record Resumen(
      Contacto contacto, String ultimoTexto, String fecha, boolean ultimoEsPropio, int sinLeer) {

    public boolean tieneSinLeer() {
      return sinLeer > 0;
    }
  }
}
