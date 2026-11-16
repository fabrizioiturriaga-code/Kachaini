package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.model.Estudiante;
import kanichi.model.FactorXp;
import kanichi.model.MultiplicadorXp;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.MultiplicadorXpRepository;
import kanichi.service.MultiplicadorXpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MultiplicadorXpServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

  private final MultiplicadorXpRepository repository = mock(MultiplicadorXpRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final MultiplicadorXpService servicio =
      new MultiplicadorXpService(repository, estudiantes, Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante deTercero;
  private Estudiante otroDeTercero;
  private Estudiante deCuarto;

  @BeforeEach
  void setup() {
    deTercero = estudiante(1L, 3, "A");
    otroDeTercero = estudiante(2L, 3, "B");
    deCuarto = estudiante(3L, 4, "A");
    when(estudiantes.findAllById(any()))
        .thenAnswer(
            invocacion -> {
              Iterable<Long> ids = invocacion.getArgument(0);
              List<Estudiante> todos = List.of(deTercero, otroDeTercero, deCuarto);
              return todos.stream()
                  .filter(
                      e -> {
                        for (Long id : ids) {
                          if (id.equals(e.getId())) {
                            return true;
                          }
                        }
                        return false;
                      })
                  .toList();
            });
  }

  @Test
  void elFactorMultiplicaLaXp() {
    assertEquals(30, FactorXp.X1_5.aplicar(20));
    assertEquals(40, FactorXp.X2.aplicar(20));
    assertEquals(60, FactorXp.X3.aplicar(20));
  }

  @Test
  void unMultiplicadorActivoAumentaLaXp() {
    when(repository.findById(1L))
        .thenReturn(
            Optional.of(
                new MultiplicadorXp(
                    1L, 9L, FactorXp.X2, AHORA.minusSeconds(60), AHORA.plusSeconds(60))));

    assertEquals(40, servicio.calcular(1L, 20, AHORA));
  }

  @Test
  void sinMultiplicadorOVencidoLaXpNoCambia() {
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertEquals(20, servicio.calcular(1L, 20, AHORA));

    when(repository.findById(2L))
        .thenReturn(
            Optional.of(
                new MultiplicadorXp(
                    2L, 9L, FactorXp.X3, AHORA.minusSeconds(120), AHORA.minusSeconds(60))));
    assertEquals(20, servicio.calcular(2L, 20, AHORA));
  }

  @Test
  void asignarCreaElMultiplicadorPorElPeriodoIndicado() {
    when(repository.findById(any())).thenReturn(Optional.empty());

    int asignados = servicio.asignar(9L, 3, List.of(1L, 2L), FactorXp.X2, 5);

    assertEquals(2, asignados);
    ArgumentCaptor<MultiplicadorXp> guardado = ArgumentCaptor.forClass(MultiplicadorXp.class);
    verify(repository, times(2)).save(guardado.capture());
    MultiplicadorXp primero = guardado.getAllValues().get(0);
    assertEquals(FactorXp.X2, primero.getFactor());
    assertEquals(9L, primero.getDocenteId());
    assertEquals(AHORA.plus(Duration.ofDays(5)), primero.getFin());
  }

  @Test
  void asignarDeNuevoReemplazaElMultiplicador() {
    MultiplicadorXp existente =
        new MultiplicadorXp(1L, 9L, FactorXp.X1_5, AHORA.minusSeconds(60), AHORA.plusSeconds(60));
    when(repository.findById(1L)).thenReturn(Optional.of(existente));

    servicio.asignar(7L, 3, List.of(1L), FactorXp.X3, 2);

    assertEquals(FactorXp.X3, existente.getFactor());
    assertEquals(7L, existente.getDocenteId());
    assertEquals(AHORA.plus(Duration.ofDays(2)), existente.getFin());
    verify(repository, never()).save(any());
  }

  @Test
  void noSeMezclanEstudiantesDeOtroGrado() {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> servicio.asignar(9L, 3, List.of(1L, 3L), FactorXp.X2, 5));

    assertEquals("Solo puedes elegir estudiantes del grado seleccionado.", error.getMessage());
    verify(repository, never()).save(any());
  }

  @Test
  void seValidaLaSeleccionElFactorYLaDuracion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.asignar(9L, 3, List.of(), FactorXp.X2, 5));
    assertThrows(
        IllegalArgumentException.class, () -> servicio.asignar(9L, 3, List.of(1L), null, 5));
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.asignar(9L, 3, List.of(1L), FactorXp.X2, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.asignar(9L, 3, List.of(1L), FactorXp.X2, 31));
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.asignar(9L, 3, List.of(99L), FactorXp.X2, 5));
  }

  @Test
  void retirarEliminaLosMultiplicadoresDeLosSeleccionados() {
    MultiplicadorXp existente =
        new MultiplicadorXp(1L, 9L, FactorXp.X2, AHORA.minusSeconds(60), AHORA.plusSeconds(60));
    when(repository.findByEstudianteIdIn(any())).thenReturn(List.of(existente));

    assertEquals(1, servicio.retirar(3, List.of(1L, 2L)));
    verify(repository).deleteAll(List.of(existente));
    assertThrows(IllegalArgumentException.class, () -> servicio.retirar(4, List.of(1L)));
  }

  @Test
  void soloSeDevuelvenLosMultiplicadoresActivos() {
    MultiplicadorXp activo =
        new MultiplicadorXp(1L, 9L, FactorXp.X2, AHORA.minusSeconds(60), AHORA.plusSeconds(60));
    MultiplicadorXp vencido =
        new MultiplicadorXp(2L, 9L, FactorXp.X2, AHORA.minusSeconds(120), AHORA.minusSeconds(1));
    when(repository.findByEstudianteIdIn(any())).thenReturn(List.of(activo, vencido));

    var activos = servicio.activos(List.of(1L, 2L));

    assertEquals(1, activos.size());
    assertSame(activo, activos.get(1L));
  }

  private static Estudiante estudiante(Long id, int grado, String seccion) {
    Estudiante estudiante =
        new Estudiante("Estudiante " + id, id + "@example.org", "hash", grado, seccion);
    ReflectionTestUtils.setField(estudiante, "id", id);
    return estudiante;
  }

  private String errorAsignar(int grado, List<Long> ids, FactorXp factor, Integer dias) {
    return assertThrows(
            IllegalArgumentException.class, () -> servicio.asignar(9L, grado, ids, factor, dias))
        .getMessage();
  }

  @Test
  void cbDiasNulo() {
    assertEquals("La duración debe ser de 1 a 30 días.", errorAsignar(3, List.of(1L), FactorXp.X2, null));
  }

  @Test
  void cnAsignacionValida() {
    assertEquals(1, servicio.asignar(9L, 3, List.of(1L), FactorXp.X2, 5));
  }

  @Test
  void cnSinEstudiantes() {
    assertEquals("Selecciona al menos un estudiante.", errorAsignar(3, List.of(), FactorXp.X2, 5));
  }

  @Test
  void cnEstudianteDeOtroGrado() {
    assertEquals(
        "Solo puedes elegir estudiantes del grado seleccionado.",
        errorAsignar(3, List.of(3L), FactorXp.X2, 5));
  }

  @Test
  void cnSinFactor() {
    assertEquals("Elige el multiplicador que quieres asignar.", errorAsignar(3, List.of(1L), null, 5));
  }

  @Test
  void cnDiasCero() {
    assertEquals("La duración debe ser de 1 a 30 días.", errorAsignar(3, List.of(1L), FactorXp.X2, 0));
  }

  @Test
  void cnDiasTreintaYUno() {
    assertEquals("La duración debe ser de 1 a 30 días.", errorAsignar(3, List.of(1L), FactorXp.X3, 31));
  }

  @Test
  void cnLimitesDeDias() {
    assertEquals(1, servicio.asignar(9L, 3, List.of(1L), FactorXp.X1_5, 1));
    assertEquals(2, servicio.asignar(9L, 3, List.of(1L, 2L), FactorXp.X3, 30));
  }

  @Test
  void cnRetirar() {
    MultiplicadorXp existente =
        new MultiplicadorXp(1L, 9L, FactorXp.X2, AHORA.minusSeconds(60), AHORA.plusSeconds(60));
    when(repository.findByEstudianteIdIn(any())).thenReturn(List.of(existente));
    assertEquals(1, servicio.retirar(3, List.of(1L)));
  }
}
