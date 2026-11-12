package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.exception.DesafioInvalidoException;
import kanichi.model.Desafio;
import kanichi.model.Estudiante;
import kanichi.model.Logro;
import kanichi.model.TipoDesafio;
import kanichi.repository.ActividadRepository;
import kanichi.repository.ActividadRepository.Avance;
import kanichi.repository.DesafioRepository;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.UsuarioRepository;
import kanichi.service.DesafioService;
import kanichi.service.LigaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DesafioServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

  private final DesafioRepository desafios = mock(DesafioRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  private final ActividadRepository actividad = mock(ActividadRepository.class);
  private final LigaService ligas = mock(LigaService.class);
  private final EntityManager entityManager = mock(EntityManager.class);
  private final DesafioService service =
      new DesafioService(
          desafios,
          estudiantes,
          usuarios,
          actividad,
          ligas,
          entityManager,
          Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private Estudiante luis;

  @BeforeEach
  void setup() {
    ana = estudiante(1L, "Ana");
    luis = estudiante(2L, "Luis");
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.findById(2L)).thenReturn(Optional.of(luis));
    when(estudiantes.findAllById(any())).thenReturn(List.of(ana, luis));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "A")).thenReturn(List.of(ana, luis));
    when(actividad.tieneActividad(1L)).thenReturn(true);
    when(ligas.evaluar(any(), any(), anyBoolean())).thenReturn(List.of());
  }

  @Test
  void sinActividadNoSePuedeCrearUnDesafio() {
    when(actividad.tieneActividad(1L)).thenReturn(false);

    assertThrows(
        DesafioInvalidoException.class,
        () -> service.crear(1L, 2L, null, TipoDesafio.DUO_EN_MARCHA));
    verify(desafios, never()).save(any());
  }

  @Test
  void noSePuedeDesafiarASiMismo() {
    assertThrows(
        DesafioInvalidoException.class,
        () -> service.crear(1L, 1L, null, TipoDesafio.DUO_EN_MARCHA));
  }

  @Test
  void noSeDuplicaUnDesafioAbiertoConElMismoCompanero() {
    when(desafios.existeEntre(eq(1L), eq(2L), any())).thenReturn(true);

    assertThrows(
        DesafioInvalidoException.class,
        () -> service.crear(1L, 2L, null, TipoDesafio.DUO_EN_MARCHA));
  }

  @Test
  void crearDejaElDesafioPendienteHastaQueElCompaneroAcepte() {
    service.crear(1L, 2L, null, TipoDesafio.EQUIPO_EXPERTO);

    ArgumentCaptor<Desafio> captor = ArgumentCaptor.forClass(Desafio.class);
    verify(desafios).save(captor.capture());
    assertEquals(Desafio.Estado.PENDIENTE, captor.getValue().getEstado());
    assertEquals(2L, captor.getValue().getCompaneroId());
    assertEquals(TipoDesafio.EQUIPO_EXPERTO, captor.getValue().getTipo());
  }

  @Test
  void sePuedeInvitarAOtroEstudianteDelSistemaPorCorreo() {
    when(usuarios.findByCorreo("luis@example.org")).thenReturn(Optional.of(luis));

    service.crear(1L, null, " LUIS@example.org ", TipoDesafio.DUO_EN_MARCHA);

    verify(desafios).save(argThat(d -> d.getCompaneroId().equals(2L)));
  }

  @Test
  void soloElInvitadoPuedeAceptar() {
    Desafio desafio = new Desafio(1L, 2L, TipoDesafio.DUO_EN_MARCHA, AHORA);
    when(desafios.bloquearPorId(10L)).thenReturn(Optional.of(desafio));

    assertThrows(DesafioInvalidoException.class, () -> service.aceptar(1L, 10L));
    service.aceptar(2L, 10L);

    assertEquals(Desafio.Estado.ACTIVO, desafio.getEstado());
    assertEquals(AHORA, desafio.getIniciado());
  }

  @Test
  void alCumplirLaMetaSeCompletaYAmbosRecibenLaRecompensa() {
    Desafio desafio = activo(TipoDesafio.DUO_EN_MARCHA);
    when(actividad.avanceEntre(eq(1L), any(), any())).thenReturn(new Avance(2, 5, 5));
    when(actividad.avanceEntre(eq(2L), any(), any())).thenReturn(new Avance(2, 4, 5));

    var panel = service.panel(1L);

    assertEquals(Desafio.Estado.COMPLETADO, desafio.getEstado());
    int xp = TipoDesafio.DUO_EN_MARCHA.getXpRecompensa();
    assertEquals(xp, ana.getXp());
    assertEquals(xp, luis.getXp());
    assertTrue(ana.tieneLogro(Logro.TRABAJO_EN_EQUIPO));
    assertTrue(luis.tieneLogro(Logro.TRABAJO_EN_EQUIPO));
    verify(estudiantes).registrarXp(1L, xp, AHORA);
    verify(estudiantes).registrarXp(2L, xp, AHORA);
    assertEquals(1, panel.notificaciones().size());

    var vista = panel.desafios().get(0);
    assertEquals(4, vista.tareas());
    assertEquals(90, vista.porcentaje());
    assertEquals(2, vista.participantes().size());
    assertTrue(vista.participantes().get(0).esYo());
  }

  @Test
  void sinElPorcentajeDeAciertosElDesafioSigueActivo() {
    Desafio desafio = activo(TipoDesafio.DUO_EN_MARCHA);
    when(actividad.avanceEntre(eq(1L), any(), any())).thenReturn(new Avance(3, 3, 5));
    when(actividad.avanceEntre(eq(2L), any(), any())).thenReturn(new Avance(2, 3, 5));

    var panel = service.panel(1L);

    assertEquals(Desafio.Estado.ACTIVO, desafio.getEstado());
    assertEquals(0, ana.getXp());
    assertEquals(4, panel.desafios().get(0).tareasEnBarra());
    assertFalse(panel.desafios().get(0).precisionAlcanzada());
  }

  @Test
  void lasInvitacionesRecibidasSeMuestranAparte() {
    Desafio invitacion = new Desafio(2L, 1L, TipoDesafio.DUO_EN_MARCHA, AHORA);
    when(desafios.deEstudiante(eq(1L), any())).thenReturn(List.of(invitacion));

    var panel = service.panel(1L);

    assertEquals(1, panel.invitaciones().size());
    assertTrue(panel.desafios().isEmpty());
    assertEquals(List.of("Luis"), panel.companeros().stream().map(c -> c.nombre()).toList());
  }

  private Desafio activo(TipoDesafio tipo) {
    Desafio desafio = new Desafio(1L, 2L, tipo, AHORA.minusSeconds(3600));
    desafio.aceptar(AHORA.minusSeconds(1800));
    when(desafios.deEstudiante(eq(1L), any())).thenReturn(List.of(desafio));
    return desafio;
  }

  private static Estudiante estudiante(Long id, String nombre) {
    Estudiante estudiante =
        spy(new Estudiante(nombre, nombre.toLowerCase() + "@example.org", "hash", 1, "A"));
    doReturn(id).when(estudiante).getId();
    return estudiante;
  }
}
