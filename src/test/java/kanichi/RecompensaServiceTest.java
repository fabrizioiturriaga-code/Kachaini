package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.dto.NivelEstudiante;
import kanichi.dto.RecompensaObtenida;
import kanichi.model.Coleccionable;
import kanichi.model.Estudiante;
import kanichi.model.Lectura;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.model.TipoPractica;
import kanichi.repository.ActividadRepository;
import kanichi.repository.ActividadRepository.FilaHistorial;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.PracticaRealizadaRepository;
import kanichi.repository.RecompensaPracticaRepository;
import kanichi.service.LigaService;
import kanichi.service.RecompensaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecompensaServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");
  private static final String CODIGO = Lectura.HOMBRE_DE_LAURICOCHA.getCodigo();

  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final PracticaRealizadaRepository practicas = mock(PracticaRealizadaRepository.class);
  private final RecompensaPracticaRepository recompensas = mock(RecompensaPracticaRepository.class);
  private final ActividadRepository actividad = mock(ActividadRepository.class);
  private final LigaService ligas = mock(LigaService.class);
  private final RecompensaService service =
      new RecompensaService(
          estudiantes, practicas, recompensas, actividad, ligas, Clock.fixed(AHORA, ZoneOffset.UTC));
  private Estudiante ana;

  @BeforeEach
  void setup() {
    ana = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(estudiantes.bloquearPorId(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(ligas.evaluar(any(), any(), anyBoolean())).thenReturn(List.of());
  }

  @Test
  void conMenosDel80PorCientoNoHayRecompensa() {
    RecompensaObtenida resultado = registrar(3, 4);

    assertFalse(resultado.aprobada());
    assertFalse(resultado.otorgada());
    assertEquals(75, resultado.porcentaje());
    assertEquals(0, ana.getXp());
    verify(practicas).save(any());
    verify(recompensas, never()).save(any());
  }

  @Test
  void conElOchentaPorCientoExactoSeApruebaYOtorgaRecompensas() {
    RecompensaObtenida resultado = registrar(4, 5);

    assertTrue(resultado.aprobada());
    assertEquals(RecompensaService.XP_POR_PRACTICA, resultado.xp());
    assertEquals(Coleccionable.PUNTA_DE_PROYECTIL, resultado.coleccionable());
    assertTrue(resultado.logros().contains(Logro.PRACTICA_DESTACADA));
    assertEquals(RecompensaService.XP_POR_PRACTICA, ana.getXp());
    assertTrue(ana.tieneLogro(Logro.PRACTICA_DESTACADA));
    verify(estudiantes).registrarXp(any(), eq(RecompensaService.XP_POR_PRACTICA), eq(AHORA));
    verify(recompensas).save(any());
    verify(ligas).evaluar(ana, AHORA, true);
  }

  @Test
  void laMismaPracticaSoloSeRecompensaUnaVez() {
    when(recompensas.existsByEstudianteIdAndTipoAndCodigo(any(), eq(TipoPractica.LECTURA), eq(CODIGO)))
        .thenReturn(true);

    RecompensaObtenida resultado = registrar(4, 4);

    assertTrue(resultado.yaRecompensada());
    assertFalse(resultado.otorgada());
    assertEquals(0, ana.getXp());
  }

  @Test
  void elColeccionableDeLaUnidadSeEntregaUnaSolaVez() {
    when(recompensas.existsByEstudianteIdAndColeccionable(any(), eq(Coleccionable.PUNTA_DE_PROYECTIL)))
        .thenReturn(true);

    RecompensaObtenida resultado = registrar(4, 4);

    assertTrue(resultado.otorgada());
    assertNull(resultado.coleccionable());
  }

  @Test
  void calificacionInvalidaEsRechazada() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.registrarPractica(1L, TipoPractica.LECTURA, CODIGO, 1, 5, 4));
  }

  @Test
  void resumenMuestraNivelColeccionablesEHistorial() {
    ana.sumarXp(130);
    when(recompensas.coleccionablesDe(any())).thenReturn(List.of(Coleccionable.QUIPU));
    when(actividad.historial(any(), anyInt()))
        .thenReturn(
            List.of(
                new FilaHistorial(
                    "PRACTICA", "LECTURA:" + CODIGO, AHORA, 15, "PUNTA_DE_PROYECTIL"),
                new FilaHistorial("INSIGNIA", "PRACTICA_DESTACADA", AHORA, null, null),
                new FilaHistorial("MISION", "BIENVENIDA", AHORA, null, null),
                new FilaHistorial("LECCION", "YA_NO_EXISTE", AHORA, null, null)));

    var resumen = service.resumen(1L);

    assertEquals(2, resumen.nivel().numero());
    assertEquals(70, resumen.nivel().xpParaSiguiente());
    assertEquals(List.of(Coleccionable.QUIPU), resumen.coleccionables());
    assertEquals(3, resumen.historial().size());
    assertTrue(resumen.historial().get(0).descripcion().startsWith("Lectura aprobada: "));
    assertTrue(resumen.historial().get(0).descripcion().contains("Punta de proyectil"));
    assertEquals("Insignia obtenida: Estudiante destacado", resumen.historial().get(1).descripcion());
    assertEquals(MisionInicial.BIENVENIDA.getXp(), resumen.historial().get(2).xp());
  }

  @Test
  void nivelSubeCadaCienXp() {
    assertEquals(1, NivelEstudiante.de(0).numero());
    assertEquals(1, NivelEstudiante.de(99).numero());
    assertEquals(2, NivelEstudiante.de(100).numero());
    assertEquals(50, NivelEstudiante.de(250).xpEnNivel());
  }

  private RecompensaObtenida registrar(int aciertos, int total) {
    return service.registrarPractica(1L, TipoPractica.LECTURA, CODIGO, 1, aciertos, total);
  }
}
