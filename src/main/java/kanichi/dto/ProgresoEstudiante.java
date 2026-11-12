package kanichi.dto;

import java.util.List;

public record ProgresoEstudiante(
    int xp,
    int misionesCompletadas,
    int leccionesCompletadas,
    int diasRacha,
    long aciertos,
    long errores,
    List<UnidadMapa> unidades) {

  public boolean tieneIntentosEvaluados() {
    return aciertos + errores > 0;
  }

  public long porcentajeAciertos() {
    return tieneIntentosEvaluados() ? Math.round(100.0 * aciertos / (aciertos + errores)) : 0;
  }
}
