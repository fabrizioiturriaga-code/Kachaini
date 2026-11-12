package kanichi.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kanichi.model.Coleccionable;

public record ResumenRecompensas(
    NivelEstudiante nivel, List<Coleccionable> coleccionables, List<Entrada> historial) {

  public int totalColeccionables() {
    return Coleccionable.values().length;
  }

  public record Entrada(Instant fecha, String icono, String descripcion, Integer xp) {
    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Lima"));

    public String fechaTexto() {
      return FORMATO.format(fecha);
    }
  }
}
