package kanichi.dto;

import java.util.List;

public record DetalleProgresoAlumno(
    ProgresoAlumno resumen, int grado, List<UnidadMapa> unidades, List<AvanceDiario> semana) {

  public int xpSemana() {
    return semana.stream().mapToInt(AvanceDiario::xp).sum();
  }

  public record AvanceDiario(String dia, int xp, int porcentaje) {}
}
