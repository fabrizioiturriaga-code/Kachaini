package kanichi.service;

import java.time.Instant;

@FunctionalInterface
public interface CalculadoraXp {
  CalculadoraXp SIN_MULTIPLICADOR = (estudianteId, xpBase, ahora) -> xpBase;

  int calcular(Long estudianteId, int xpBase, Instant ahora);
}
