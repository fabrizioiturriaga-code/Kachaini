package kanichi.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Logro;

public final class EvaluadorLogros {
  private EvaluadorLogros() {}

  public static List<Logro> otorgarNuevos(Estudiante estudiante, Instant ahora) {
    var completadas = estudiante.getLeccionesCompletadas();
    List<Integer> unidades = Leccion.unidadesDelGrado(estudiante.getGrado());

    Map<Logro, Boolean> requisitos = new LinkedHashMap<>();
    requisitos.put(Logro.APRENDIZ_DE_HISTORIA, !completadas.isEmpty());
    requisitos.put(
        Logro.UNIDAD_DOMINADA,
        unidades.stream().anyMatch(u -> completadas.containsAll(Leccion.deUnidad(u))));
    requisitos.put(Logro.CIEN_XP, estudiante.getXp() >= Logro.XP_PARA_CIEN_XP);
    requisitos.put(
        Logro.RACHA_DE_TRES, estudiante.rachaEn(ahora).dias() >= Logro.DIAS_PARA_CONSTANCIA);
    requisitos.put(
        Logro.MAESTRO_DEL_GRADO,
        unidades.stream().allMatch(u -> completadas.containsAll(Leccion.deUnidad(u))));

    List<Logro> nuevos = new ArrayList<>();
    requisitos.forEach(
        (logro, cumplido) -> {
          if (cumplido && estudiante.otorgarLogro(logro)) {
            nuevos.add(logro);
          }
        });
    return nuevos;
  }
}