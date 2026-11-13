package kanichi.dto;

import java.util.List;
import kanichi.model.RetoDocente;

public record RetoPublicado(
    RetoDocente reto, long notificados, long leidos, List<String> completaron) {
  public int completados() {
    return completaron.size();
  }
}