package kanichi.dto;

public record ProgresoAlumno(
    Long id,
    String nombre,
    String avatar,
    String seccion,
    int porcentajeAvance,
    int misionesCompletadas,
    int leccionesCompletadas,
    int xp,
    int diasRacha,
    String multiplicador,
    String multiplicadorHasta) {

  public boolean tieneMultiplicador() {
    return multiplicador != null;
  }
}
