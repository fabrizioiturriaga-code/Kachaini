package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.dto.Conversacion;
import kanichi.dto.Conversacion.MensajeChat;
import kanichi.dto.Conversacion.Resumen;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.MensajeDirecto;
import kanichi.repository.DocenteRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.MensajeDirectoRepository;
import kanichi.service.MensajeriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class MensajeriaServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

  private final MensajeDirectoRepository mensajes = mock(MensajeDirectoRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final DocenteRepository docentes = mock(DocenteRepository.class);
  private final MensajeriaService servicio =
      new MensajeriaService(mensajes, estudiantes, docentes, Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private Estudiante beto;
  private Estudiante carla;
  private Docente luz;

  @BeforeEach
  void setup() {
    ana = estudiante(1L, "Ana", 1, "A");
    beto = estudiante(2L, "Beto", 1, "B");
    carla = estudiante(3L, "Carla", 2, "A");
    luz = docente(10L, "Luz", true);
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(anyInt(), anyString()))
        .thenReturn(List.of());
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "A")).thenReturn(List.of(ana));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "B")).thenReturn(List.of(beto));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "A")).thenReturn(List.of(carla));
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(docentes.findById(10L)).thenReturn(Optional.of(luz));
  }

  @Test
  void elDocenteEscribeATodoElSalonDeUnGrado() {
    int enviados = servicio.enviarAEstudiantes(10L, 1, null, null, true, "  Repasen la unidad 1 ");

    assertEquals(2, enviados);
    List<MensajeDirecto> guardados = capturarGuardados();
    assertEquals(List.of(1L, 2L), guardados.stream().map(MensajeDirecto::getEstudianteId).toList());
    assertTrue(guardados.stream().allMatch(MensajeDirecto::isEnviadoPorDocente));
    assertTrue(guardados.stream().allMatch(m -> m.getTexto().equals("Repasen la unidad 1")));
    assertTrue(guardados.stream().allMatch(m -> m.getDocenteId().equals(10L)));
    verify(estudiantes, never()).findByGradoAndSeccionOrderByNombreAsc(eq(2), anyString());
  }

  @Test
  void elDocenteEscribeAUnaSeccion() {
    assertEquals(1, servicio.enviarAEstudiantes(10L, 1, "B", null, true, "Hola"));

    assertEquals(
        List.of(2L), capturarGuardados().stream().map(MensajeDirecto::getEstudianteId).toList());
  }

  @Test
  void elDocenteEscribeALosEstudiantesElegidos() {
    assertEquals(1, servicio.enviarAEstudiantes(10L, 1, null, List.of(2L), false, "Hola Beto"));

    assertEquals(
        List.of(2L), capturarGuardados().stream().map(MensajeDirecto::getEstudianteId).toList());
  }

  @Test
  void noSeMezclanEstudiantesDeOtroGrado() {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> servicio.enviarAEstudiantes(10L, 1, null, List.of(1L, 3L), false, "Hola"));

    assertEquals("Solo puedes elegir estudiantes del grado seleccionado.", error.getMessage());
    verify(mensajes, never()).saveAll(anyIterable());
  }

  @Test
  void elEnvioValidaElTextoElGradoYLaSeleccion() {
    assertEquals(
        "Escribe un mensaje antes de enviarlo.",
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.enviarAEstudiantes(10L, 1, null, null, true, "   "))
            .getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.enviarAEstudiantes(10L, 1, null, null, true, "x".repeat(1001)));
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.enviarAEstudiantes(10L, 6, null, null, true, "Hola"));
    assertEquals(
        "Elige al menos un estudiante o envía el mensaje a todo el salón.",
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.enviarAEstudiantes(10L, 1, null, List.of(), false, "Hola"))
            .getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.enviarAEstudiantes(10L, 4, null, null, true, "Hola"));
    verify(mensajes, never()).saveAll(anyIterable());
  }

  @Test
  void laBandejaDelDocenteAgrupaPorEstudianteYCuentaLosNoLeidos() {
    MensajeDirecto respuesta = MensajeDirecto.deEstudiante(1L, 10L, "Tengo una duda", AHORA);
    MensajeDirecto otra =
        MensajeDirecto.deEstudiante(1L, 10L, "¿Me ayuda?", AHORA.minusSeconds(60));
    MensajeDirecto inicial =
        MensajeDirecto.deDocente(10L, 1L, "Hola Ana", AHORA.minusSeconds(120));
    MensajeDirecto aBeto =
        MensajeDirecto.deDocente(10L, 2L, "Hola Beto", AHORA.minusSeconds(180));
    when(mensajes.findByDocenteIdOrderByEnviadoDescIdDesc(10L))
        .thenReturn(List.of(respuesta, otra, inicial, aBeto));
    when(estudiantes.findAllById(any())).thenReturn(List.of(ana, beto));

    List<Resumen> bandeja = servicio.bandejaDocente(10L);

    assertEquals(2, bandeja.size());
    assertEquals("Ana", bandeja.get(0).contacto().nombre());
    assertEquals("1.° A", bandeja.get(0).contacto().detalle());
    assertEquals("Tengo una duda", bandeja.get(0).ultimoTexto());
    assertEquals("12/11 10:00", bandeja.get(0).fecha());
    assertEquals(2, bandeja.get(0).sinLeer());
    assertFalse(bandeja.get(0).ultimoEsPropio());
    assertEquals(0, bandeja.get(1).sinLeer());
    assertTrue(bandeja.get(1).ultimoEsPropio());
  }

  @Test
  void abrirLaConversacionMarcaComoLeidosLosMensajesDelOtro() {
    when(mensajes.findByDocenteIdAndEstudianteIdOrderByEnviadoAscIdAsc(10L, 1L))
        .thenReturn(
            List.of(
                MensajeDirecto.deDocente(10L, 1L, "Hola Ana", AHORA.minusSeconds(60)),
                MensajeDirecto.deEstudiante(1L, 10L, "Hola profe", AHORA)));

    Conversacion delDocente = servicio.conversacionDocente(10L, 1L);
    Conversacion delEstudiante = servicio.conversacionEstudiante(1L, 10L);

    verify(mensajes).marcarLeidos(10L, 1L, false, AHORA);
    verify(mensajes).marcarLeidos(10L, 1L, true, AHORA);
    assertEquals(
        List.of(true, false), delDocente.mensajes().stream().map(MensajeChat::propio).toList());
    assertEquals(
        List.of(false, true), delEstudiante.mensajes().stream().map(MensajeChat::propio).toList());
    assertEquals("Luz", delEstudiante.contacto().nombre());
    assertEquals("Historia", delEstudiante.contacto().detalle());
  }

  @Test
  void unaConversacionConAlguienQueNoExisteDaNoEncontrado() {
    when(estudiantes.findById(99L)).thenReturn(Optional.empty());
    when(docentes.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> servicio.conversacionDocente(10L, 99L));
    assertThrows(ResponseStatusException.class, () -> servicio.conversacionEstudiante(1L, 99L));
    assertThrows(
        ResponseStatusException.class, () -> servicio.responderComoDocente(10L, 99L, "Hola"));
    verify(mensajes, never()).save(any());
  }

  @Test
  void elEstudianteRespondeASuDocente() {
    servicio.enviarComoEstudiante(1L, 10L, "Gracias, profe");

    ArgumentCaptor<MensajeDirecto> guardado = ArgumentCaptor.forClass(MensajeDirecto.class);
    verify(mensajes).save(guardado.capture());
    assertFalse(guardado.getValue().isEnviadoPorDocente());
    assertEquals(1L, guardado.getValue().getEstudianteId());
    assertEquals(10L, guardado.getValue().getDocenteId());
    assertEquals(AHORA, guardado.getValue().getEnviado());
  }

  @Test
  void elEstudianteSoloEscribeADocentesAutorizados() {
    Docente pendiente = docente(11L, "Raúl", false);
    when(docentes.findById(11L)).thenReturn(Optional.of(pendiente));

    assertEquals(
        "El docente elegido no está disponible.",
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.enviarComoEstudiante(1L, 11L, "Hola"))
            .getMessage());
    assertThrows(
        IllegalArgumentException.class, () -> servicio.enviarComoEstudiante(1L, null, "Hola"));
    verify(mensajes, never()).save(any());
  }

  @Test
  void losNoLeidosSeCuentanSegunElRol() {
    when(mensajes.countByDocenteIdAndEnviadoPorDocenteFalseAndLeidoIsNull(10L)).thenReturn(3L);
    when(mensajes.countByEstudianteIdAndEnviadoPorDocenteTrueAndLeidoIsNull(1L)).thenReturn(2L);

    assertEquals(3, servicio.sinLeerDocente(10L));
    assertEquals(2, servicio.sinLeerEstudiante(1L));
  }

  @Test
  void laVistaPreviaSeRecorta() {
    when(mensajes.findByEstudianteIdOrderByEnviadoDescIdDesc(1L))
        .thenReturn(List.of(MensajeDirecto.deDocente(10L, 1L, "a ".repeat(100), AHORA)));
    when(docentes.findAllById(any())).thenReturn(List.of(luz));

    Resumen resumen = servicio.bandejaEstudiante(1L).get(0);

    assertEquals(MensajeriaService.LARGO_VISTA_PREVIA, resumen.ultimoTexto().length());
    assertTrue(resumen.ultimoTexto().endsWith("…"));
    assertEquals(1, resumen.sinLeer());
  }

  @SuppressWarnings("unchecked")
  private List<MensajeDirecto> capturarGuardados() {
    ArgumentCaptor<List<MensajeDirecto>> captor = ArgumentCaptor.forClass(List.class);
    verify(mensajes).saveAll(captor.capture());
    return captor.getValue();
  }

  private static Estudiante estudiante(Long id, String nombre, int grado, String seccion) {
    Estudiante estudiante = new Estudiante(nombre, id + "@example.org", "hash", grado, seccion);
    ReflectionTestUtils.setField(estudiante, "id", id);
    return estudiante;
  }

  private static Docente docente(Long id, String nombre, boolean autorizado) {
    Docente docente = new Docente(nombre, id + "@example.org", "hash", "Historia");
    ReflectionTestUtils.setField(docente, "id", id);
    ReflectionTestUtils.setField(docente, "autorizado", autorizado);
    return docente;
  }
}
