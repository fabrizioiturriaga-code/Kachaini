package kanichi.dto;

import java.util.List;
import kanichi.model.Coleccionable;
import kanichi.model.Logro;

public record RecompensaObtenida(
    int porcentaje,
    boolean aprobada,
    boolean yaRecompensada,
    int xp,
    Coleccionable coleccionable,
    List<Logro> logros,
    List<String> notificaciones) {

  public RecompensaObtenida {
    logros = List.copyOf(logros);
    notificaciones = List.copyOf(notificaciones);
  }

  public static RecompensaObtenida sinRecompensa(
      int porcentaje, boolean aprobada, boolean yaRecompensada) {
    return new RecompensaObtenida(
        porcentaje, aprobada, yaRecompensada, 0, null, List.of(), List.of());
  }

  public boolean otorgada() {
    return xp > 0;
  }
}
