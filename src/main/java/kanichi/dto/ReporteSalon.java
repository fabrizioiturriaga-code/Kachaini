package kanichi.dto;

import java.util.List;
import kanichi.dto.DetalleProgresoAlumno.AvanceDiario;

public record ReporteSalon(
    int grado,
    String seccion,
    String generado,
    int promedioAvance,
    int xpTotal,
    int xpSemana,
    int xpSemanaAnterior,
    int activosSemana,
    int promedioRacha,
    Integer porcentajeAciertos,
    List<AvanceDiario> semana,
    List<UnidadReporte> unidades,
    List<FilaReporte> filas,
    List<FilaReporte> destacados,
    List<FilaReporte> requierenApoyo,
    List<String> observaciones) {

  public String salon() {
    return seccion == null
        ? grado + ".° de secundaria, todas las secciones"
        : grado + ".° " + seccion;
  }

  public int totalEstudiantes() {
    return filas.size();
  }

  public boolean isVacio() {
    return filas.isEmpty();
  }

  public int porcentajeActivos() {
    return filas.isEmpty() ? 0 : activosSemana * 100 / filas.size();
  }

  public Integer variacionSemanal() {
    if (xpSemanaAnterior == 0) {
      return null;
    }
    return Math.round((xpSemana - xpSemanaAnterior) * 100f / xpSemanaAnterior);
  }

  public record FilaReporte(
      Long id,
      String nombre,
      String avatar,
      String seccion,
      int porcentajeAvance,
      int leccionesCompletadas,
      int xp,
      int xpSemana,
      int diasRacha,
      long aciertos,
      long errores,
      String motivoApoyo) {

    public Integer porcentajeAciertos() {
      long intentos = aciertos + errores;
      return intentos == 0 ? null : (int) (aciertos * 100 / intentos);
    }

    public boolean necesitaApoyo() {
      return motivoApoyo != null;
    }
  }

  public record UnidadReporte(
      int numero, String titulo, int completaron, int enCurso, int sinEmpezar) {

    public int total() {
      return completaron + enCurso + sinEmpezar;
    }

    public int porcentaje() {
      return total() == 0 ? 0 : completaron * 100 / total();
    }
  }
}
