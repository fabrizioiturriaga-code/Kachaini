package kanichi.dto;

import java.util.List;
import kanichi.model.Desafio;
import kanichi.model.TipoDesafio;

public record PanelDesafios(
    boolean puedeDesafiar,
    List<Companero> companeros,
    List<VistaDesafio> invitaciones,
    List<VistaDesafio> desafios,
    List<String> notificaciones) {

  public List<TipoDesafio> tipos() {
    return List.of(TipoDesafio.values());
  }

  public record Companero(Long id, String nombre, String avatar) {}

  public record Participante(
      String nombre, String avatar, long tareas, int porcentaje, boolean esYo) {}

  public record VistaDesafio(
      Long id,
      TipoDesafio tipo,
      Desafio.Estado estado,
      List<Participante> participantes,
      long tareas,
      int porcentaje,
      boolean soyInvitado) {

    public long tareasEnBarra() {
      return Math.min(tareas, tipo.getMetaTareas());
    }

    public boolean precisionAlcanzada() {
      return porcentaje >= tipo.getPrecisionMinima();
    }

    public String claseEstado() {
      return "desafio--" + estado.name().toLowerCase();
    }
  }
}
