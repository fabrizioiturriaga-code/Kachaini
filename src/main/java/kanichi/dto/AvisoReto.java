package kanichi.dto;

import kanichi.model.RetoDocente;

public record AvisoReto(
    RetoDocente reto,
    boolean leida,
    boolean completado,
    RetoDocente.Estado estado,
    boolean fallido) {

  public AvisoReto(
      RetoDocente reto, boolean leida, boolean completado, RetoDocente.Estado estado) {
    this(reto, leida, completado, estado, false);
  }

  public boolean isPorVencer() {
    return !completado && !fallido && estado == RetoDocente.Estado.POR_VENCER;
  }

  public boolean isVencido() {
    return !completado && !fallido && estado == RetoDocente.Estado.VENCIDO;
  }

  public boolean isPuedeCompletarse() {
    return !completado && !fallido && estado != RetoDocente.Estado.VENCIDO;
  }
}