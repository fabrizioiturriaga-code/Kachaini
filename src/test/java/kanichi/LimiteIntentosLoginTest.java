package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import kanichi.security.LimiteIntentosLogin;
import org.junit.jupiter.api.Test;

class LimiteIntentosLoginTest {
  @Test
  void limitaSoloLaDireccionQueSuperaElMaximo() {
    var limite = new LimiteIntentosLogin(2, Duration.ofSeconds(60), Clock.systemUTC());
    limite.registrarFallo("direccion-a");
    assertEquals(0, limite.segundosRestantes("direccion-a"));
    limite.registrarFallo("direccion-a");
    assertTrue(limite.segundosRestantes("direccion-a") > 0);
    assertEquals(0, limite.segundosRestantes("direccion-b"));
  }

  @Test
  void permiteReintentarCuandoVenceLaEspera() {
    Clock reloj = org.mockito.Mockito.mock(Clock.class);
    Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
    org.mockito.Mockito.when(reloj.instant()).thenReturn(inicio);
    var limite = new LimiteIntentosLogin(1, Duration.ofSeconds(60), reloj);
    limite.registrarFallo("direccion-a");
    assertEquals(60, limite.segundosRestantes("direccion-a"));
    org.mockito.Mockito.when(reloj.instant()).thenReturn(inicio.plusSeconds(60));
    assertEquals(0, limite.segundosRestantes("direccion-a"));
    limite.registrarFallo("direccion-a");
    assertEquals(60, limite.segundosRestantes("direccion-a"));
  }

  @Test
  void rechazaConfiguracionesInvalidas() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LimiteIntentosLogin(0, Duration.ofSeconds(60), Clock.systemUTC()));
  }

  @Test
  void produccionExigeDominiosConfirmados() {
    assertThrows(
        IllegalArgumentException.class, () -> new kanichi.validation.PoliticaCorreo("", true));
    assertTrue(
        new kanichi.validation.PoliticaCorreo("colegio.edu.pe", true)
            .permite("ana@colegio.edu.pe"));
  }
}
