package kanichi.dto;

import kanichi.model.RetoDocente;

public record AvisoReto(RetoDocente reto, boolean leida, RetoDocente.Estado estado) {
  public boolean isPorVencer() {
    return estado == RetoDocente.Estado.POR_VENCER;
  }

  public boolean isVencido() {
    return estado == RetoDocente.Estado.VENCIDO;
  }
}