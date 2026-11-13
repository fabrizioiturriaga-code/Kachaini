package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import kanichi.dto.ResultadoReto;
import kanichi.model.Estudiante;
import kanichi.model.Notificacion;
import kanichi.model.RetoDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.NotificacionRepository;
import kanichi.repository.RetoDocenteRepository;
import kanichi.service.ForoService;
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
  private final ForoService foroService = mock(ForoService.class);
  private final RetoDocenteService service =
      new RetoDocenteService(
          retos, notificaciones, estudiantes, foroService, Clock.fixed(AHORA, ZoneOffset.UTC));

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
  void completarDaLaXpYLoAnunciaEnElForoCuandoTodoElSalonTermino() {
    Estudiante ana = estudianteDelSalon(1L, 2, "A");
    Estudiante beto = estudianteDelSalon(2L, 2, "A");
    Notificacion notificacion = new Notificacion(1L, 10L, AHORA);
    RetoDocente reto = reto(10L, HOY.plusDays(5));
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.bloquearSalon(2, "A")).thenReturn(List.of(ana, beto));
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
        .thenReturn(Optional.of(notificacion));
    when(retos.findById(10L)).thenReturn(Optional.of(reto));
    when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(0L);
    when(foroService.anunciarRetoCompletado(reto, 2, "A")).thenReturn(true);

    ResultadoReto resultado = service.completar(1L, 10L);

    assertEquals(20, resultado.xp());
    assertTrue(resultado.salonCompleto());
    assertTrue(notificacion.isCompletada());
    assertTrue(notificacion.isLeida());
    verify(ana).sumarXp(20);
    verify(estudiantes).registrarXp(1L, 20, AHORA);
    verify(foroService).anunciarRetoCompletado(reto, 2, "A");
    assertEquals(
        "¡Reto completado! Ganaste 20 XP. Todo tu salón lo completó: lo anunciamos en el foro 🎉",
        resultado.mensaje());
  }

   @Test
  void siFaltanCompanerosNoSePublicaEnElForo() {
    Estudiante ana = estudianteDelSalon(1L, 2, "A");
    Notificacion notificacion = new Notificacion(1L, 10L, AHORA);
    RetoDocente reto = reto(10L, HOY.plusDays(5));
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.bloquearSalon(2, "A")).thenReturn(List.of(ana));
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
        .thenReturn(Optional.of(notificacion));
    when(retos.findById(10L)).thenReturn(Optional.of(reto));
    when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(1L);

    ResultadoReto resultado = service.completar(1L, 10L);

    assertFalse(resultado.salonCompleto());
    assertEquals("¡Reto completado! Ganaste 20 XP.", resultado.mensaje());
    verifyNoInteractions(foroService);
  }

    @Test
  void noSePuedeCompletarDosVecesNiUnRetoVencido() {
    Estudiante ana = estudianteDelSalon(1L, 2, "A");
    Notificacion yaCompletada = new Notificacion(1L, 10L, AHORA);
    yaCompletada.completar(AHORA);
    Notificacion pendiente = new Notificacion(1L, 11L, AHORA);
    RetoDocente vigente = reto(10L, HOY.plusDays(5));
    RetoDocente vencido = reto(11L, HOY.minusDays(1));
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.bloquearSalon(2, "A")).thenReturn(List.of(ana));
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
        .thenReturn(Optional.of(yaCompletada));
    when(retos.findById(10L)).thenReturn(Optional.of(vigente));
    when(notificaciones.findByEstudianteIdAndRetoId(1L, 11L))
        .thenReturn(Optional.of(pendiente));
    when(retos.findById(11L)).thenReturn(Optional.of(vencido));

    assertError("Ya completaste este reto.", () -> service.completar(1L, 10L));
    assertError("Este reto ya venció.", () -> service.completar(1L, 11L));
    verify(ana, never()).sumarXp(anyInt());
    verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
  }

  @Test
  void elDocenteVeQuienesCompletaronSuReto() {
    Estudiante ana = estudianteDelSalon(1L, 2, "A");
    when(ana.getNombre()).thenReturn("Ana");
    RetoDocente reto = reto(10L, HOY.plusDays(5));
    Notificacion completada = new Notificacion(1L, 10L, AHORA);
    completada.completar(AHORA);
    when(retos.findByDocenteIdOrderByPublicadoDesc(9L)).thenReturn(List.of(reto));
    when(notificaciones.countByRetoId(10L)).thenReturn(3L);
    when(notificaciones.countByRetoIdAndLeidaIsNotNull(10L)).thenReturn(2L);
    when(notificaciones.findByRetoIdAndCompletadaIsNotNullOrderByCompletadaAsc(10L))
        .thenReturn(List.of(completada));
    when(estudiantes.findAllById(List.of(1L))).thenReturn(List.of(ana));

    var publicado = service.publicadosPor(9L).get(0);

    assertEquals(3, publicado.notificados());
    assertEquals(2, publicado.leidos());
    assertEquals(List.of("Ana"), publicado.completaron());
    assertEquals(1, publicado.completados());
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

  private static Estudiante estudianteDelSalon(Long id, int grado, String seccion) {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(id);
    when(estudiante.getGrado()).thenReturn(grado);
    when(estudiante.getSeccion()).thenReturn(seccion);
    return estudiante;
  }

  private static Estudiante estudiante(Long id) {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(id);
    return estudiante;
  }
}