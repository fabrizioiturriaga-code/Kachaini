package kanichi.dto;

import kanichi.model.RetoDocente;

public record AvisoReto(
    RetoDocente reto, boolean leida, boolean completado, RetoDocente.Estado estado) {
  public boolean isPorVencer() {
    return !completado && estado == RetoDocente.Estado.POR_VENCER;
  }

  public boolean isVencido() {
    return !completado && estado == RetoDocente.Estado.VENCIDO;
  }

  public boolean isPuedeCompletarse() {
    return !completado && estado != RetoDocente.Estado.VENCIDO;
  }
}