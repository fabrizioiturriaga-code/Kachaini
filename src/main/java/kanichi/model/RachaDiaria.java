package kanichi.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public record RachaDiaria(int dias, boolean completadaHoy) {
  private static final ZoneId ZONA = ZoneId.of("America/Lima");

  static RachaDiaria calcular(Collection<Instant> fechas, Instant ahora) {
    LocalDate hoy = LocalDate.ofInstant(ahora, ZONA);
    Set<LocalDate> diasConLeccion = new HashSet<>();
    for (Instant fecha : fechas) {
      diasConLeccion.add(LocalDate.ofInstant(fecha, ZONA));
    }

    boolean completadaHoy = diasConLeccion.contains(hoy);
    LocalDate dia = completadaHoy ? hoy : hoy.minusDays(1);
    int dias = 0;
    while (diasConLeccion.contains(dia)) {
      dias++;
      dia = dia.minusDays(1);
    }
    return new RachaDiaria(dias, completadaHoy);
  }
}
