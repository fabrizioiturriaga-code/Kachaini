package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.UnidadMapa;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.LeccionDocente;
import kanichi.model.UnidadDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.LeccionDocenteRepository;
import kanichi.repository.UnidadDocenteRepository;
import kanichi.service.LeccionDocenteService;
import kanichi.service.UnidadDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class LeccionDocenteServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-13T15:00:00Z");

  private final UnidadDocenteRepository unidades = mock(UnidadDocenteRepository.class);
  private final LeccionDocenteRepository lecciones = mock(LeccionDocenteRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final LeccionDocenteService service =
      new LeccionDocenteService(
          unidades,
          lecciones,
          estudiantes,
          (id, xpBase, ahora) -> xpBase * 2,
          Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private UnidadDocente unidad;
  private LeccionDocente primera;
  private LeccionDocente segunda;
  private Set<Long> completadas;

  @BeforeEach
  void setup() {
    ana = new Estudiante("Ana", "ana@example.org", "hash", 2, "A");
    ana.completarOnboarding();
    ReflectionTestUtils.setField(ana, "id", 1L);
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.bloquearPorId(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.registrarIntento(eq(1L), any(), anyBoolean())).thenReturn(1);

    unidad = new UnidadDocente(10L, 2, "Mi unidad", AHORA);
    ReflectionTestUtils.setField(unidad, "id", 7L);
    primera = leccion(30L, 1);
    segunda = leccion(31L, 2);
    when(unidades.findByGradoOrderByIdAsc(2)).thenReturn(List.of(unidad));
    when(unidades.findById(7L)).thenReturn(Optional.of(unidad));
    when(lecciones.findById(30L)).thenReturn(Optional.of(primera));
    when(lecciones.findById(31L)).thenReturn(Optional.of(segunda));
    when(lecciones.findByUnidadIdOrderByOrdenAsc(7L)).thenReturn(List.of(primera, segunda));
    when(lecciones.findByUnidadIdInOrderByUnidadIdAscOrdenAsc(any()))
        .thenReturn(List.of(primera, segunda));
    completadas = new HashSet<>();
    when(lecciones.completadasPor(1L)).thenAnswer(i -> new HashSet<>(completadas));
  }

  @Test
  void laUnidadApareceEnElMapaDespuesDeLasOficialesDelGrado() {
    var mapa = service.unidadesDelMapa(1L);

    assertEquals(1, mapa.size());
    assertEquals(7L, mapa.get(0).id());
    assertEquals(3, mapa.get(0).mapa().numero());
    assertEquals("Mi unidad", mapa.get(0).mapa().titulo());
    assertEquals(UnidadMapa.Estado.ACTIVA, mapa.get(0).mapa().estado());
    assertEquals(2, mapa.get(0).mapa().leccionesTotal());
  }

  @Test
  void soloSeConsultanLasUnidadesDelGradoDelEstudiante() {
    service.unidadesDelMapa(1L);

    verify(unidades).findByGradoOrderByIdAsc(2);
    verify(unidades, never()).findByGradoOrderByIdAsc(1);
  }

  @Test
  void unaLeccionDeOtroGradoNoExisteParaElEstudiante() {
    unidad.actualizar(3, "Mi unidad", AHORA);

    assertThrows(ResponseStatusException.class, () -> service.obtenerLeccion(1L, 30L));
    assertThrows(
        ResponseStatusException.class,
        () -> service.responder(1L, 30L, 0, UUID.randomUUID()));
  }

  @Test
  void lasLeccionesSeHacenEnOrden() {
    assertFalse(service.obtenerLeccion(1L, 31L).disponible());
    assertThrows(
        MisionNoPermitidaException.class,
        () -> service.responder(1L, 31L, 0, UUID.randomUUID()));
  }

  @Test
  void respuestaCorrectaOtorgaXpConMultiplicador() {
    var resultado = service.responder(1L, 30L, 0, UUID.randomUUID());

    assertEquals(Resultado.CORRECTA, resultado.resultado());
    assertEquals(LeccionDocente.XP * 2, resultado.xpGanada());
    assertFalse(resultado.unidadCompletada());
    assertEquals(LeccionDocente.XP * 2, ana.getXp());
    verify(lecciones).registrarCompletada(1L, 30L, AHORA);
    verify(estudiantes).registrarXp(1L, LeccionDocente.XP * 2, AHORA);
  }

  @Test
  void completarLaUltimaLeccionCompletaLaUnidad() {
    completadas.add(30L);

    var resultado = service.responder(1L, 31L, 0, UUID.randomUUID());

    assertTrue(resultado.unidadCompletada());
  }

  @Test
  void respuestaIncorrectaRestaEnergiaSinXp() {
    var resultado = service.responder(1L, 30L, 2, UUID.randomUUID());

    assertEquals(Resultado.INCORRECTA, resultado.resultado());
    assertEquals(4, resultado.energia().actual());
    assertEquals(0, ana.getXp());
    verify(lecciones, never()).registrarCompletada(anyLong(), anyLong(), any());
  }

  @Test
  void unaLeccionCompletadaNoSeVuelveAPremiar() {
    completadas.add(30L);

    var resultado = service.responder(1L, 30L, 0, UUID.randomUUID());

    assertEquals(Resultado.YA_COMPLETADA, resultado.resultado());
    verify(estudiantes, never()).registrarXp(anyLong(), anyInt(), any());
  }

  private static LeccionDocente leccion(Long id, int orden) {
    LeccionDocente leccion =
        new LeccionDocente(
            7L, orden, UnidadDocenteService.validar(UnidadDocenteServiceTest.formulario()));
    ReflectionTestUtils.setField(leccion, "id", id);
    return leccion;
  }
}
