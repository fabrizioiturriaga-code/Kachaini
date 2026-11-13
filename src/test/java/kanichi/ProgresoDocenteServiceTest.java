package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kanichi.dto.DetalleProgresoAlumno;
import kanichi.dto.ProgresoAlumno;
import kanichi.model.Estudiante;
import kanichi.model.FactorXp;
import kanichi.model.Leccion;
import kanichi.model.MultiplicadorXp;
import kanichi.repository.EstudianteRepository;
import kanichi.service.MapaMisionesService;
import kanichi.service.MultiplicadorXpService;
import kanichi.service.ProgresoDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class ProgresoDocenteServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final MultiplicadorXpService multiplicadores = mock(MultiplicadorXpService.class);
  private final ProgresoDocenteService servicio =
      new ProgresoDocenteService(
          estudiantes,
          new MapaMisionesService(),
          multiplicadores,
          Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private Estudiante beto;

  @BeforeEach
  void setup() {
    ana = estudiante(1L, "Ana", 1, "A");
    ana.completarOnboarding();
    ana.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, AHORA.minusSeconds(3600));
    ana.registrarLeccion(Leccion.U1_CAZADORES_DEL_PERU, AHORA.minusSeconds(1800));
    ana.registrarLeccion(Leccion.U1_AGRICULTURA, AHORA.minusSeconds(600));
    beto = estudiante(2L, "Beto", 1, "B");
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(anyInt(), anyString()))
        .thenReturn(List.of());
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "A")).thenReturn(List.of(ana));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "B")).thenReturn(List.of(beto));
    when(multiplicadores.activos(any())).thenReturn(Map.of());
  }

  @Test
  void laListaMuestraLosIndicadoresDeCadaEstudiante() {
    when(multiplicadores.activos(any()))
        .thenReturn(
            Map.of(
                1L,
                new MultiplicadorXp(
                    1L, 9L, FactorXp.X2, AHORA.minusSeconds(60), AHORA.plusSeconds(86400))));

    List<ProgresoAlumno> lista = servicio.listar(1, "A");

    assertEquals(1, lista.size());
    ProgresoAlumno alumno = lista.get(0);
    assertEquals("Ana", alumno.nombre());
    assertEquals(50, alumno.porcentajeAvance());
    assertEquals(3, alumno.leccionesCompletadas());
    assertEquals(60, alumno.xp());
    assertEquals(1, alumno.diasRacha());
    assertEquals("2x", alumno.multiplicador());
    assertEquals("13/11/2026 10:00", alumno.multiplicadorHasta());
  }

  @Test
  void sinSeccionSeListanTodasLasSeccionesDelMismoGrado() {
    List<ProgresoAlumno> lista = servicio.listar(1, null);

    assertEquals(List.of("Ana", "Beto"), lista.stream().map(ProgresoAlumno::nombre).toList());
    verify(estudiantes, never()).findByGradoAndSeccionOrderByNombreAsc(eq(2), anyString());
    assertFalse(lista.get(1).tieneMultiplicador());
  }

  @Test
  void elGradoDebeSerValido() {
    assertThrows(IllegalArgumentException.class, () -> servicio.listar(6, "A"));
    assertNull(ProgresoDocenteService.normalizarSeccion("Z"));
    assertEquals("B", ProgresoDocenteService.normalizarSeccion(" b "));
  }

  @Test
  void elDetalleTieneElAvanceDeLosUltimosSieteDias() {
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.sumarXpEntre(eq(1L), any(), any())).thenReturn(0);
    when(estudiantes.sumarXpEntre(
            eq(1L),
            eq(Instant.parse("2026-11-12T05:00:00Z")),
            eq(Instant.parse("2026-11-13T05:00:00Z"))))
        .thenReturn(60);
    when(estudiantes.sumarXpEntre(
            eq(1L),
            eq(Instant.parse("2026-11-11T05:00:00Z")),
            eq(Instant.parse("2026-11-12T05:00:00Z"))))
        .thenReturn(30);

    DetalleProgresoAlumno detalle = servicio.detalle(1L);

    assertEquals(7, detalle.semana().size());
    assertEquals(90, detalle.xpSemana());
    assertEquals(100, detalle.semana().get(6).porcentaje());
    assertEquals(50, detalle.semana().get(5).porcentaje());
    assertEquals(0, detalle.semana().get(0).porcentaje());
    assertEquals(List.of(1, 2), detalle.unidades().stream().map(u -> u.numero()).toList());
    assertTrue(detalle.unidades().get(0).isCompletada());
  }

  @Test
  void unEstudianteInexistenteDaNoEncontrado() {
    when(estudiantes.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> servicio.detalle(99L));
  }

  private static Estudiante estudiante(Long id, String nombre, int grado, String seccion) {
    Estudiante estudiante = new Estudiante(nombre, id + "@example.org", "hash", grado, seccion);
    ReflectionTestUtils.setField(estudiante, "id", id);
    return estudiante;
  }
}
