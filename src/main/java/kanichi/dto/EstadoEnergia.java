package kanichi.dto;

/**
 * HU-12: energía de aprendizaje del estudiante en un instante dado.
 *
 * @param actual unidades de energía disponibles
 * @param maxima capacidad máxima de la barra
 * @param segundosParaRecarga segundos que faltan para recuperar la siguiente unidad (0 si la
 *     barra está llena)
 */
public record EstadoEnergia(int actual, int maxima, long segundosParaRecarga) {

  public boolean isAgotada() {
    return actual <= 0;
  }

  public boolean isRecargando() {
    return actual < maxima;
  }

  /** Minutos (redondeados hacia arriba) para la siguiente unidad de energía. */
  public long minutosParaRecarga() {
    return (segundosParaRecarga + 59) / 60;
  }
}
