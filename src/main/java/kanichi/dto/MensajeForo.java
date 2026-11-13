package kanichi.dto;

import kanichi.model.TipoPublicacion;

public record MensajeForo(
    String autor,
    String avatar,
    String texto,
    String fecha,
    TipoPublicacion tipo,
    boolean propio) {
  public boolean isAutomatico() {
    return tipo.isAutomatica();
  }
}