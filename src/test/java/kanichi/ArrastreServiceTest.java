package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import kanichi.dto.ActividadPractica;
import kanichi.dto.ResultadoArrastre;
import kanichi.model.ActividadArrastre;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EstudianteRepository;
import kanichi.service.ArrastreService;
import kanichi.service.MapaMisionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ArrastreServiceTest {
  private static final ActividadArrastre ACTIVIDAD = ActividadArrastre.LEGADO_PREINCA;

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final ArrastreService servicio = new ArrastreService(repository, new MapaMisionesService());
  private Estudiante estudiante;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
  }

  @Test
  void cadaElementoEnSuLugarEsUnResultadoPerfecto() {
    ResultadoArrastre resultado = servicio.corregir(ACTIVIDAD, List.of(0, 1, 2, 3));

    assertEquals(4, resultado.aciertos());
    assertTrue(resultado.isPerfecto());
    assertEquals("La ciudad más antigua de América", resultado.emparejamientos().get(0).destino());
    assertEquals("Caral", resultado.emparejamientos().get(0).elegido());
  }

  @Test
  void indicaQueEmparejamientosSonCorrectosYCualesNo() {
    ResultadoArrastre resultado = servicio.corregir(ACTIVIDAD, List.of(1, 0, 2, 3));

    assertEquals(2, resultado.aciertos());
    assertFalse(resultado.isPerfecto());
    assertFalse(resultado.esCorrecto(0));
    assertFalse(resultado.esCorrecto(1));
    assertTrue(resultado.esCorrecto(2));
    assertTrue(resultado.esCorrecto(3));
    assertEquals("Chavín", resultado.emparejamientos().get(0).elegido());
  }

  @Test
  void unElementoInexistenteEsIncorrecto() {
    ResultadoArrastre resultado = servicio.corregir(ACTIVIDAD, List.of(9, 1, 2, 3));

    assertFalse(resultado.esCorrecto(0));
    assertEquals("-", resultado.emparejamientos().get(0).elegido());
  }

  @Test
  void noSeVerificaSiFaltanElementosPorUbicar() {
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.corregir(ACTIVIDAD, Arrays.asList(0, null, 2, 3)));
    assertThrows(IllegalArgumentException.class, () -> servicio.corregir(ACTIVIDAD, List.of(0)));
    assertThrows(IllegalArgumentException.class, () -> servicio.corregir(ACTIVIDAD, null));
  }

  @Test
  void lasActividadesSeDesbloqueanConSuUnidad() {
    assertTrue(servicio.listar(1L).stream().noneMatch(ActividadPractica::disponible));

    estudiante.completarOnboarding();
    assertTrue(servicio.estaDisponible(1L, ActividadArrastre.LUGARES_DEL_POBLAMIENTO));
    assertFalse(servicio.estaDisponible(1L, ACTIVIDAD));

    Leccion.deUnidad(1).forEach(leccion -> estudiante.registrarLeccion(leccion, Instant.EPOCH));

    assertTrue(servicio.estaDisponible(1L, ACTIVIDAD));
    assertEquals(
        List.of(true, true),
        servicio.listar(1L).stream().map(ActividadPractica::disponible).toList());
  }

  @Test
  void soloSeListanLasActividadesDelGradoDelEstudiante() {
    Estudiante deQuinto = new Estudiante("Rosa", "rosa@example.org", "hash", 5, "C");
    deQuinto.completarOnboarding();
    when(repository.findById(2L)).thenReturn(Optional.of(deQuinto));

    assertEquals(
        List.of("peru-siglo-xx", "peru-contemporaneo"),
        servicio.listar(2L).stream().map(ActividadPractica::codigo).toList());
    assertTrue(servicio.estaDisponible(2L, ActividadArrastre.PERU_SIGLO_XX));
    assertThrows(
        ResponseStatusException.class,
        () -> servicio.estaDisponible(2L, ActividadArrastre.LUGARES_DEL_POBLAMIENTO));
  }
}
