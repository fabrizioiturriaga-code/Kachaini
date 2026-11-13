package kanichi.dto;

import java.util.List;
import kanichi.model.LeccionDocente;
import kanichi.model.UnidadDocente;

public final class UnidadesDocente {
  private UnidadesDocente() {}

  public record Resumen(UnidadDocente unidad, long lecciones) {}

  public record EnMapa(Long id, UnidadMapa mapa) {}

  public record EstadoLeccion(LeccionDocente leccion, boolean completada, boolean disponible) {
    public String claseCss() {
      return completada ? "unidad--completada" : disponible ? "unidad--activa" : "unidad--bloqueada";
    }
  }

  public record VistaUnidad(EnMapa unidad, List<EstadoLeccion> lecciones, EstadoEnergia energia) {}

  public record VistaLeccion(
      LeccionDocente leccion,
      EnMapa unidad,
      boolean completada,
      boolean disponible,
      EstadoEnergia energia) {}
}
