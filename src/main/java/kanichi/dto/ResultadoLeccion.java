package kanichi.dto;

/**
 * Resultado de responder la pregunta de una lección.
 *
 * @param xpGanada XP otorgada (0 salvo que la respuesta sea correcta y sea la primera vez)
 * @param energia energía del estudiante después de la respuesta
 * @param unidadCompletada si con esta lección el estudiante terminó la unidad
 * @param unidadDesbloqueada número de la unidad que se desbloquea, o null si no hay una nueva
 */
public record ResultadoLeccion(
    Resultado resultado,
    int xpGanada,
    EstadoEnergia energia,
    boolean unidadCompletada,
    Integer unidadDesbloqueada,
    int diasRacha,
    boolean rachaAumentada) {

  public enum Resultado {
    CORRECTA,
    INCORRECTA,
    INTENTO_REPETIDO,
    SIN_ENERGIA,
    YA_COMPLETADA
  }
}
