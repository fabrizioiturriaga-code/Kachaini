package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EntregaDesarrolloRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.service.CalculadoraXp;
import kanichi.service.LeccionService;
import kanichi.service.MapaMisionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeccionMultiplicadorTest {
  private static final Leccion PRIMERA = Leccion.U1_PRIMEROS_POBLADORES;
  private static final int CORRECTA = PRIMERA.getPregunta().respuestaCorrecta();
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private Estudiante estudiante;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    estudiante.completarOnboarding();
    when(repository.bloquearPorId(1L)).thenReturn(Optional.of(estudiante));
    when(repository.registrarIntento(anyLong(), any(UUID.class), anyBoolean())).thenReturn(1);
  }

  @Test
  void conMultiplicadorLaLeccionDaMasXpYCuentaParaElRanking() {
    CalculadoraXp doble = (id, xp, ahora) -> xp * 2;

    ResultadoLeccion resultado = servicio(doble).responder(1L, PRIMERA, CORRECTA, UUID.randomUUID());

    assertEquals(Resultado.CORRECTA, resultado.resultado());
    assertEquals(PRIMERA.getXp() * 2, resultado.xpGanada());
    assertEquals(PRIMERA.getXp() * 2, estudiante.getXp());
    verify(repository).registrarXp(1L, PRIMERA.getXp() * 2, AHORA);
  }

  @Test
  void sinMultiplicadorLaXpEsLaDeSiempre() {
    ResultadoLeccion resultado =
        servicio(CalculadoraXp.SIN_MULTIPLICADOR)
            .responder(1L, PRIMERA, CORRECTA, UUID.randomUUID());

    assertEquals(PRIMERA.getXp(), resultado.xpGanada());
    assertEquals(PRIMERA.getXp(), estudiante.getXp());
    verify(repository).registrarXp(1L, PRIMERA.getXp(), AHORA);
  }

  @Test
  void unaRespuestaIncorrectaNoUsaElMultiplicador() {
    CalculadoraXp calculadora = mock(CalculadoraXp.class);
    int incorrecta = (CORRECTA + 1) % 3;

    servicio(calculadora).responder(1L, PRIMERA, incorrecta, UUID.randomUUID());

    verifyNoInteractions(calculadora);
    assertEquals(0, estudiante.getXp());
  }

  private LeccionService servicio(CalculadoraXp calculadora) {
    return new LeccionService(
        repository,
        mock(EntregaDesarrolloRepository.class),
        new MapaMisionesService(),
        Clock.fixed(AHORA, ZoneOffset.UTC),
        calculadora);
  }
}
