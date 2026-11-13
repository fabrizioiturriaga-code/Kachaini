package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanichi.dto.LeccionDocenteForm;
import kanichi.model.LeccionDocente;
import kanichi.model.UnidadDocente;
import kanichi.repository.LeccionDocenteRepository;
import kanichi.repository.UnidadDocenteRepository;
import kanichi.service.UnidadDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class UnidadDocenteServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-13T15:00:00Z");

  private final UnidadDocenteRepository unidades = mock(UnidadDocenteRepository.class);
  private final LeccionDocenteRepository lecciones = mock(LeccionDocenteRepository.class);
  private final UnidadDocenteService service =
      new UnidadDocenteService(unidades, lecciones, Clock.fixed(AHORA, ZoneOffset.UTC));

  @BeforeEach
  void setup() {
    when(unidades.save(any()))
        .thenAnswer(
            invocacion -> {
              UnidadDocente unidad = invocacion.getArgument(0);
              ReflectionTestUtils.setField(unidad, "id", 7L);
              return unidad;
            });
    when(lecciones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
  }

  @Test
  void crearGuardaLaUnidadConSuPrimeraLeccion() {
    Long id = service.crear(10L, 2, "  Mi unidad  ", formulario());

    assertEquals(7L, id);
    ArgumentCaptor<UnidadDocente> unidad = ArgumentCaptor.forClass(UnidadDocente.class);
    verify(unidades).save(unidad.capture());
    assertEquals(2, unidad.getValue().getGrado());
    assertEquals("Mi unidad", unidad.getValue().getTitulo());
    ArgumentCaptor<LeccionDocente> leccion = ArgumentCaptor.forClass(LeccionDocente.class);
    verify(lecciones).save(leccion.capture());
    assertEquals(7L, leccion.getValue().getUnidadId());
    assertEquals(1, leccion.getValue().getOrden());
    assertEquals(List.of("Inca", "Maya", "Azteca"), leccion.getValue().getOpciones());
    assertEquals(0, leccion.getValue().getRespuestaCorrecta());
  }

  @Test
  void rechazaGradoInvalido() {
    assertThrows(
        IllegalArgumentException.class, () -> service.crear(10L, 6, "Unidad", formulario()));
    verify(unidades, never()).save(any());
  }

  @Test
  void rechazaLeccionSinRespuestaCorrecta() {
    LeccionDocenteForm formulario = formulario();
    formulario.setRespuestaCorrecta(null);

    var error =
        assertThrows(
            IllegalArgumentException.class, () -> service.crear(10L, 1, "Unidad", formulario));
    assertTrue(error.getMessage().contains("correcta"));
  }

  @Test
  void rechazaOpcionesRepetidas() {
    LeccionDocenteForm formulario = formulario();
    formulario.setOpcionC("inca");

    assertThrows(
        IllegalArgumentException.class, () -> service.crear(10L, 1, "Unidad", formulario));
  }

  @Test
  void rechazaCamposVaciosOLargos() {
    LeccionDocenteForm sinPregunta = formulario();
    sinPregunta.setPregunta("  ");
    assertThrows(
        IllegalArgumentException.class, () -> service.crear(10L, 1, "Unidad", sinPregunta));

    LeccionDocenteForm largo = formulario();
    largo.setTitulo("x".repeat(LeccionDocente.MAXIMO_TITULO + 1));
    assertThrows(IllegalArgumentException.class, () -> service.crear(10L, 1, "Unidad", largo));
  }

  @Test
  void unDocenteNoPuedeEditarLaUnidadDeOtro() {
    UnidadDocente ajena = unidad(20L);
    when(unidades.findById(7L)).thenReturn(Optional.of(ajena));

    assertThrows(
        ResponseStatusException.class, () -> service.actualizarUnidad(10L, 7L, 1, "Nuevo"));
    assertThrows(
        ResponseStatusException.class, () -> service.agregarLeccion(10L, 7L, formulario()));
  }

  @Test
  void editarUnidadCambiaGradoYTitulo() {
    UnidadDocente propia = unidad(10L);
    when(unidades.findById(7L)).thenReturn(Optional.of(propia));

    service.actualizarUnidad(10L, 7L, 4, "Otra unidad");

    assertEquals(4, propia.getGrado());
    assertEquals("Otra unidad", propia.getTitulo());
  }

  @Test
  void agregarLeccionLaPoneAlFinalYRespetaElMaximo() {
    when(unidades.findById(7L)).thenReturn(Optional.of(unidad(10L)));
    when(lecciones.countByUnidadId(7L)).thenReturn(2L);

    service.agregarLeccion(10L, 7L, formulario());

    ArgumentCaptor<LeccionDocente> leccion = ArgumentCaptor.forClass(LeccionDocente.class);
    verify(lecciones).save(leccion.capture());
    assertEquals(3, leccion.getValue().getOrden());

    when(lecciones.countByUnidadId(7L)).thenReturn((long) UnidadDocenteService.MAXIMO_LECCIONES);
    assertThrows(
        IllegalArgumentException.class, () -> service.agregarLeccion(10L, 7L, formulario()));
  }

  @Test
  void editarLeccionActualizaSusDatos() {
    when(unidades.findById(7L)).thenReturn(Optional.of(unidad(10L)));
    LeccionDocente leccion = leccion(30L, 1);
    when(lecciones.findById(30L)).thenReturn(Optional.of(leccion));
    LeccionDocenteForm cambios = formulario();
    cambios.setTitulo("Título nuevo");
    cambios.setRespuestaCorrecta(2);

    service.actualizarLeccion(10L, 7L, 30L, cambios);

    assertEquals("Título nuevo", leccion.getTitulo());
    assertEquals(2, leccion.getRespuestaCorrecta());
  }

  @Test
  void eliminarLeccionReordenaLasDemasYNoDejaLaUnidadVacia() {
    when(unidades.findById(7L)).thenReturn(Optional.of(unidad(10L)));
    LeccionDocente primera = leccion(30L, 1);
    LeccionDocente segunda = leccion(31L, 2);
    LeccionDocente tercera = leccion(32L, 3);
    when(lecciones.findById(31L)).thenReturn(Optional.of(segunda));
    when(lecciones.findByUnidadIdOrderByOrdenAsc(7L))
        .thenReturn(new ArrayList<>(List.of(primera, segunda, tercera)));

    service.eliminarLeccion(10L, 7L, 31L);

    verify(lecciones).delete(segunda);
    assertEquals(1, primera.getOrden());
    assertEquals(2, tercera.getOrden());

    when(lecciones.findById(30L)).thenReturn(Optional.of(primera));
    when(lecciones.findByUnidadIdOrderByOrdenAsc(7L)).thenReturn(List.of(primera));
    assertThrows(IllegalArgumentException.class, () -> service.eliminarLeccion(10L, 7L, 30L));
  }

  @Test
  void elContenidoSeMuestraPorParrafos() {
    LeccionDocente leccion = leccion(30L, 1);
    assertEquals(List.of("Primer párrafo.", "Segundo párrafo."), leccion.getParrafos());
  }

  static LeccionDocenteForm formulario() {
    LeccionDocenteForm formulario = new LeccionDocenteForm();
    formulario.setTitulo("Los incas");
    formulario.setContenido("Primer párrafo.\n\n  Segundo párrafo.  ");
    formulario.setPregunta("¿Quiénes construyeron Machu Picchu?");
    formulario.setOpcionA("Inca");
    formulario.setOpcionB("Maya");
    formulario.setOpcionC("Azteca");
    formulario.setRespuestaCorrecta(0);
    return formulario;
  }

  private static UnidadDocente unidad(Long docenteId) {
    UnidadDocente unidad = new UnidadDocente(docenteId, 1, "Unidad", AHORA);
    ReflectionTestUtils.setField(unidad, "id", 7L);
    return unidad;
  }

  private static LeccionDocente leccion(Long id, int orden) {
    LeccionDocente leccion =
        new LeccionDocente(7L, orden, UnidadDocenteService.validar(formulario()));
    ReflectionTestUtils.setField(leccion, "id", id);
    return leccion;
  }
}
