package kanichi.dto;

import java.util.List;
import kanichi.model.Leccion;

/** Detalle de una unidad: sus lecciones con su estado y la energía del estudiante. */
public record VistaUnidad(UnidadMapa unidad, List<EstadoLeccion> lecciones, EstadoEnergia energia) {

  public record EstadoLeccion(Leccion leccion, boolean completada, boolean disponible) {
    public String claseCss() {
      return completada ? "unidad--completada" : disponible ? "unidad--activa" : "unidad--bloqueada";
    }
  }
}
