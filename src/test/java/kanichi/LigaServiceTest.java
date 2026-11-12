package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.model.CambioLiga;
import kanichi.model.Estudiante;
import kanichi.model.Liga;
import kanichi.repository.CambioLigaRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.service.LigaService;
import kanichi.service.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LigaServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");
  private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);

  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final CambioLigaRepository cambios = mock(CambioLigaRepository.class);
  private final LigaService service =
      new LigaService(estudiantes, cambios, Clock.fixed(AHORA, ZoneOffset.UTC));
  private Estudiante ana;

  @BeforeEach
  void setup() {
    ana = spy(new Estudiante("Ana", "ana@example.org", "hash", 1, "A"));
    doReturn(1L).when(ana).getId();
    when(cambios.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
  }

  @Test
  void asciendeAlAlcanzarElUmbralDeLaSemanaYNotifica() {
    xpEnSemana(LUNES, 45);

    List<CambioLiga> resultado = service.evaluar(ana, AHORA, false);

    assertEquals(Liga.PANACA, ana.getLiga());
    assertEquals(1, resultado.size());
    assertTrue(resultado.get(0).isAscenso());
    assertTrue(resultado.get(0).mensaje().contains("¡Ascendiste a Liga Panaca"));
    assertEquals(LUNES, ana.getLigaSemana());
  }

  @Test
  void puedeSubirVariasLigasDeUnaVez() {
    xpEnSemana(LUNES, 130);

    service.evaluar(ana, AHORA, false);

    assertEquals(Liga.INCA, ana.getLiga());
  }

  @Test
  void desciendeUnaLigaSiLaSemanaPasadaNoAlcanzoElMinimo() {
    ana.cambiarLiga(Liga.AUQUI);
    ana.evaluarLigaEnSemana(LUNES.minusWeeks(1));
    xpEnSemana(LUNES.minusWeeks(1), 50);

    List<CambioLiga> resultado = service.evaluar(ana, AHORA, false);

    assertEquals(Liga.PANACA, ana.getLiga());
    assertEquals(1, resultado.size());
    assertFalse(resultado.get(0).isAscenso());
    assertTrue(resultado.get(0).mensaje().contains("Descendiste a Liga Panaca"));
  }

  @Test
  void mantieneLaLigaSiLaSemanaPasadaAlcanzoElMinimo() {
    ana.cambiarLiga(Liga.AUQUI);
    ana.evaluarLigaEnSemana(LUNES.minusWeeks(1));
    xpEnSemana(LUNES.minusWeeks(1), 90);

    assertTrue(service.evaluar(ana, AHORA, false).isEmpty());
    assertEquals(Liga.AUQUI, ana.getLiga());
  }

  @Test
  void trasVariasSemanasSinActividadBajaUnaLigaPorSemana() {
    ana.cambiarLiga(Liga.INCA);
    ana.evaluarLigaEnSemana(LUNES.minusWeeks(3));

    List<CambioLiga> resultado = service.evaluar(ana, AHORA, false);

    assertEquals(3, resultado.size());
    assertEquals(Liga.AMAUTA, ana.getLiga());
  }

  @Test
  void laPrimeraEvaluacionNoBajaDeLiga() {
    ana.cambiarLiga(Liga.PANACA);

    assertTrue(service.evaluar(ana, AHORA, false).isEmpty());
    assertEquals(Liga.PANACA, ana.getLiga());
  }

  @Test
  void lasNotificacionesSeEntreganUnaSolaVez() {
    when(estudiantes.bloquearPorId(1L)).thenReturn(Optional.of(ana));
    CambioLiga pendiente = new CambioLiga(1L, Liga.AMAUTA, Liga.PANACA, AHORA, false);
    when(cambios.findByEstudianteIdAndNotificadoFalseOrderByIdAsc(1L))
        .thenReturn(List.of(pendiente));

    List<String> mensajes = service.actualizarYNotificar(1L);

    assertEquals(1, mensajes.size());
    assertTrue(mensajes.get(0).contains("Liga Panaca"));
  }

  private void xpEnSemana(LocalDate lunes, int xp) {
    when(estudiantes.sumarXpEntre(
            eq(1L),
            eq(lunes.atStartOfDay(RankingService.ZONA).toInstant()),
            eq(lunes.plusWeeks(1).atStartOfDay(RankingService.ZONA).toInstant())))
        .thenReturn(xp);
  }
}
