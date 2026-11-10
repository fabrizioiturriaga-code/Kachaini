package kanichi.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LimiteIntentosLogin {
  private final Map<String, Intentos> intentosPorDireccion = new HashMap<>();
  private final int maximoIntentos;
  private final Duration ventana;
  private final Clock reloj;
  private static final int MAXIMO_DIRECCIONES = 10000;

  @Autowired
  public LimiteIntentosLogin(
      @Value("${kaichaini.login.max-intentos:5}") int maximoIntentos,
      @Value("${kaichaini.login.espera-segundos:60}") long esperaSegundos) {
    this(maximoIntentos, Duration.ofSeconds(esperaSegundos), Clock.systemUTC());
  }

  public LimiteIntentosLogin(int maximoIntentos, Duration ventana, Clock reloj) {
    if (maximoIntentos < 1 || ventana.isNegative() || ventana.isZero()) {
      throw new IllegalArgumentException("El límite y la duración deben ser positivos.");
    }
    this.maximoIntentos = maximoIntentos;
    this.ventana = ventana;
    this.reloj = reloj;
  }

  public synchronized long segundosRestantes(String direccion) {
    limpiarVencidos();
    Intentos intentos = intentosPorDireccion.get(direccion);
    if (intentos != null && intentos.cantidad >= maximoIntentos) {
      return Math.max(
          1, (Duration.between(reloj.instant(), intentos.vence).toMillis() + 999) / 1000);
    }
    if (intentos == null && intentosPorDireccion.size() >= MAXIMO_DIRECCIONES) {
      return 1;
    }
    return 0;
  }

  public synchronized void registrarFallo(String direccion) {
    limpiarVencidos();
    Intentos anterior = intentosPorDireccion.get(direccion);
    if (anterior != null) {
      intentosPorDireccion.put(direccion, new Intentos(anterior.cantidad + 1, anterior.vence));
    } else if (intentosPorDireccion.size() < MAXIMO_DIRECCIONES) {
      intentosPorDireccion.put(direccion, new Intentos(1, reloj.instant().plus(ventana)));
    }
  }

  private void limpiarVencidos() {
    Instant ahora = reloj.instant();
    intentosPorDireccion.values().removeIf(intentos -> !ahora.isBefore(intentos.vence));
  }

  private record Intentos(int cantidad, Instant vence) {}
}
