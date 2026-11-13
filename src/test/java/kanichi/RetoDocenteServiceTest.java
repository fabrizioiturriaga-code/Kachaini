package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanichi.dto.AvisoReto;
import kanichi.model.Estudiante;
import kanichi.model.Notificacion;
import kanichi.model.RetoDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.NotificacionRepository;
import kanichi.repository.RetoDocenteRepository;
import kanichi.service.RetoDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class RetoDocenteServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T18:00:00Z");

  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

  private final RetoDocenteRepository retos = mock(RetoDocenteRepository.class);
  private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final RetoDocenteService service =
      new RetoDocenteService(
          retos, notificaciones, estudiantes, Clock.fixed(AHORA, ZoneOffset.UTC));

  @BeforeEach
  void setup() {
    Estudiante ana = estudiante(1L);
    Estudiante beto = estudiante(2L);
    Estudiante carla = estudiante(3L);
    when(retos.save(any(RetoDocente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(eq(2), anyString()))
        .thenReturn(List.of());
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "A"))
        .thenReturn(List.of(ana, beto));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "C"))
        .thenReturn(List.of(carla));
  }

  @Test
  void publicarNotificaAlInstanteATodosLosEstudiantesDelSalon() {
    int notificados = publicar("A", HOY.plusDays(7), 30);

    assertEquals(2, notificados);
    ArgumentCaptor<RetoDocente> reto = ArgumentCaptor.forClass(RetoDocente.class);
    verify(retos).save(reto.capture());
    assertEquals("Energía Inca", reto.getValue().getTitulo());
    assertEquals("A", reto.getValue().getSeccion());
    assertEquals(AHORA, reto.getValue().getPublicado());
    assertEquals(
        List.of(1L, 2L),
        notificacionesGuardadas().stream().map(Notificacion::getEstudianteId).toList());
    assertTrue(notificacionesGuardadas().stream().allMatch(n -> AHORA.equals(n.getCreada())));
    assertTrue(notificacionesGuardadas().stream().noneMatch(Notificacion::isLeida));
  }

  @Test
  void sinSeccionSeNotificaATodoElGrado() {
    assertEquals(3, publicar("", HOY.plusDays(7), 30));
    assertEquals(
        List.of(1L, 2L, 3L),
        notificacionesGuardadas().stream().map(Notificacion::getEstudianteId).toList());
  }

  @Test
  void validaLosDatosDelReto() {
    assertError(
        "La fecha límite debe ser hoy o una fecha futura.",
        () -> publicar("A", HOY.minusDays(1), 30));
    assertError("La recompensa debe ser de 0 a 100 XP.", () -> publicar("A", HOY, 101));
    assertError(
        "Selecciona un grado de 1 a 5.",
        () ->
            service.publicar(9L, 7, "A", "Reto", "Descripción", "Objetivo", HOY.plusDays(1), 10));
    assertError(
        "El título es obligatorio.",
        () -> service.publicar(9L, 2, "A", "  ", "Descripción", "Objetivo", HOY.plusDays(1), 10));
    assertError(
        "No hay estudiantes registrados en ese salón.",
        () ->
            service.publicar(9L, 2, "F", "Reto", "Descripción", "Objetivo", HOY.plusDays(1), 10));
    verify(retos, never()).save(any());
  }

  @Test
  void elResumenCuentaLosNuevosYLosProximosAVencer() {
    RetoDocente nuevo = reto(10L, HOY.plusDays(10));
    RetoDocente leidoPorVencer = reto(11L, HOY.plusDays(1));
    RetoDocente vencido = reto(12L, HOY.minusDays(1));
    Notificacion leida = new Notificacion(1L, 11L, AHORA);
    leida.marcarLeida(AHORA);
    when(notificaciones.findByEstudianteIdOrderByCreadaDesc(1L))
        .thenReturn(
            List.of(new Notificacion(1L, 10L, AHORA), leida, new Notificacion(1L, 12L, AHORA)));
    when(retos.findAllById(any())).thenReturn(List.of(nuevo, leidoPorVencer, vencido));

    var resumen = service.resumen(1L);

    assertEquals(2, resumen.nuevos());
    assertEquals(1, resumen.porVencer());
    assertEquals("Tienes 2 retos nuevos y 1 reto próximo a vencer.", resumen.texto());
    List<AvisoReto> avisos = service.avisos(1L);
    assertFalse(avisos.get(0).leida(), "Los no leídos aparecen primero");
    assertTrue(avisos.get(avisos.size() - 1).leida());
  }

  @Test
  void abrirMuestraElDetalleYLoMarcaComoLeido() {
    Notificacion notificacion = new Notificacion(1L, 10L, AHORA);
    RetoDocente reto = reto(10L, HOY.plusDays(2));
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
        .thenReturn(Optional.of(notificacion));
    when(retos.findById(10L)).thenReturn(Optional.of(reto));

    AvisoReto aviso = service.abrir(1L, 10L);

    assertTrue(notificacion.isLeida());
    assertSame(reto, aviso.reto());
    assertEquals(RetoDocente.Estado.POR_VENCER, aviso.estado());
  }

  @Test
  void unRetoQueNoSeLeNotificoAlEstudianteNoExiste() {
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 99L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> service.abrir(1L, 99L));
  }

  @Test
  void elEstadoDependeDeLaFechaLimite() {
    assertEquals(RetoDocente.Estado.VIGENTE, reto(1L, HOY.plusDays(3)).estadoEn(HOY));
    assertEquals(RetoDocente.Estado.POR_VENCER, reto(1L, HOY.plusDays(2)).estadoEn(HOY));
    assertEquals(RetoDocente.Estado.POR_VENCER, reto(1L, HOY).estadoEn(HOY));
    assertEquals(RetoDocente.Estado.VENCIDO, reto(1L, HOY.minusDays(1)).estadoEn(HOY));
  }

  @Test
  void losObjetivosSeSeparanPorLinea() {
    RetoDocente reto =
        new RetoDocente(9L, 2, null, "Reto", "Desc", "Leer\r\n\n  Responder  ", HOY, 10, AHORA);

    assertEquals(List.of("Leer", "Responder"), reto.getListaObjetivos());
    assertEquals("2.° (todas las secciones)", reto.getDestinatarios());
  }

  private int publicar(String seccion, LocalDate fecha, int xp) {
    return service.publicar(
        9L,
        2,
        seccion,
        "Energía Inca",
        "Repasa el Tahuantinsuyo.",
        "Completar la unidad 3\nResponder sin errores",
        fecha,
        xp);
  }

  @SuppressWarnings("unchecked")
  private List<Notificacion> notificacionesGuardadas() {
    ArgumentCaptor<Iterable<Notificacion>> guardadas = ArgumentCaptor.forClass(Iterable.class);
    verify(notificaciones).saveAll(guardadas.capture());
    List<Notificacion> lista = new ArrayList<>();
    guardadas.getValue().forEach(lista::add);
    return lista;
  }

  private static void assertError(String mensaje, Runnable accion) {
    var error = assertThrows(IllegalArgumentException.class, accion::run);
    assertEquals(mensaje, error.getMessage());
  }

  private static RetoDocente reto(Long id, LocalDate fechaLimite) {
    RetoDocente reto =
        spy(new RetoDocente(9L, 2, "A", "Reto " + id, "Desc", "Objetivo", fechaLimite, 20, AHORA));
    doReturn(id).when(reto).getId();
    return reto;
  }

  private static Estudiante estudiante(Long id) {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(id);
    return estudiante;
  }
}