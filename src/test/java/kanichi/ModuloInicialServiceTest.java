package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import kanichi.dto.ResultadoMision;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.repository.EstudianteRepository;
import kanichi.service.ModuloInicialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-06: misiones introductorias, recompensas y desbloqueo. */
class ModuloInicialServiceTest {
  private static final int RESPUESTA_CORRECTA =
      MisionInicial.RETO_DE_BIENVENIDA.getPregunta().respuestaCorrecta();

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final ModuloInicialService service = new ModuloInicialService(repository);
  private Estudiante estudiante;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
    when(repository.bloquearPorId(1L)).thenReturn(Optional.of(estudiante));
  }

  @Test
  void estudianteNuevoVeElModuloConSoloLaPrimeraMisionDisponible() {
    var progreso = service.obtenerProgreso(1L);

    assertFalse(progreso.completado());
    assertEquals(MisionInicial.values().length, progreso.total());
    assertEquals(0, progreso.completadas());
    assertTrue(progreso.misiones().get(0).disponible());
    assertFalse(progreso.misiones().get(1).disponible());
    assertEquals(MisionInicial.Tipo.TUTORIAL, progreso.misiones().get(0).mision().getTipo());
  }

  @Test
  void completarElTutorialOtorgaXpEInsignia() {
    ResultadoMision resultado = service.completar(1L, MisionInicial.BIENVENIDA, null);

    assertEquals(MisionInicial.BIENVENIDA.getXp(), resultado.xpGanada());
    assertEquals(List.of(Logro.PRIMEROS_PASOS), resultado.logrosNuevos());
    assertFalse(resultado.moduloCompletado());
    assertEquals(MisionInicial.BIENVENIDA.getXp(), estudiante.getXp());
    assertTrue(estudiante.tieneLogro(Logro.PRIMEROS_PASOS));
  }

  @Test
  void noSePuedeSaltarUnaMision() {
    assertThrows(
        MisionNoPermitidaException.class,
        () -> service.completar(1L, MisionInicial.LINEA_DE_TIEMPO, null));
    assertEquals(0, estudiante.getXp());
  }

  @Test
  void respuestaIncorrectaNoOtorgaRecompensa() {
    completarLecciones();
    int xpAntes = estudiante.getXp();

    assertThrows(
        MisionNoPermitidaException.class,
        () -> service.completar(1L, MisionInicial.RETO_DE_BIENVENIDA, RESPUESTA_CORRECTA + 1));
    assertThrows(
        MisionNoPermitidaException.class,
        () -> service.completar(1L, MisionInicial.RETO_DE_BIENVENIDA, null));

    assertEquals(xpAntes, estudiante.getXp());
    assertFalse(estudiante.haCompletado(MisionInicial.RETO_DE_BIENVENIDA));
  }

  @Test
  void completarTodasLasMisionesDesbloqueaElSiguienteNivel() {
    completarLecciones();

    ResultadoMision resultado =
        service.completar(1L, MisionInicial.RETO_DE_BIENVENIDA, RESPUESTA_CORRECTA);

    assertTrue(resultado.moduloCompletado());
    assertEquals(List.of(Logro.PRIMER_ACIERTO, Logro.EXPLORADOR), resultado.logrosNuevos());
    assertTrue(estudiante.isOnboardingCompletado());
    assertEquals(MisionInicial.xpTotal(), estudiante.getXp());
    assertTrue(service.obtenerProgreso(1L).completado());
  }

  @Test
  void repetirUnaMisionNoDuplicaLaRecompensa() {
    service.completar(1L, MisionInicial.BIENVENIDA, null);

    ResultadoMision repetida = service.completar(1L, MisionInicial.BIENVENIDA, null);

    assertEquals(0, repetida.xpGanada());
    assertTrue(repetida.logrosNuevos().isEmpty());
    assertEquals(MisionInicial.BIENVENIDA.getXp(), estudiante.getXp());
  }

  private void completarLecciones() {
    service.completar(1L, MisionInicial.BIENVENIDA, null);
    service.completar(1L, MisionInicial.QUE_ES_LA_HISTORIA, null);
    service.completar(1L, MisionInicial.LINEA_DE_TIEMPO, null);
  }
}
