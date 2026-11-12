package kanichi.dto;

import java.util.List;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;

public record ProgresoModulo(
    List<EstadoMision> misiones, int xp, List<Logro> logros, boolean completado) {

  public record EstadoMision(MisionInicial mision, boolean completada, boolean disponible) {
    public String claseCss() {
      return completada ? "unidad--completada" : disponible ? "unidad--activa" : "unidad--bloqueada";
    }
  }

  public long completadas() {
    return misiones.stream().filter(EstadoMision::completada).count();
  }

  public int total() {
    return misiones.size();
  }

  public boolean tieneLogro(Logro logro) {
    return logros.contains(logro);
  }
}
