package kanichi.dto;

import java.util.List;
import kanichi.model.Logro;

public record ResultadoLeccion(
    Resultado resultado,
    int xpGanada,
    EstadoEnergia energia,
    boolean unidadCompletada,
    Integer unidadDesbloqueada,
    int diasRacha,
    boolean rachaAumentada,
    List<Logro> logrosNuevos) {

  public ResultadoLeccion {
    logrosNuevos = logrosNuevos == null ? List.of() : List.copyOf(logrosNuevos);
  }

  public ResultadoLeccion(
      Resultado resultado,
      int xpGanada,
      EstadoEnergia energia,
      boolean unidadCompletada,
      Integer unidadDesbloqueada,
      int diasRacha,
      boolean rachaAumentada) {
    this(
        resultado,
        xpGanada,
        energia,
        unidadCompletada,
        unidadDesbloqueada,
        diasRacha,
        rachaAumentada,
        List.of());
  }

  public enum Resultado {
    CORRECTA,
    INCORRECTA,
    INTENTO_REPETIDO,
    SIN_ENERGIA,
    YA_COMPLETADA
  }
}