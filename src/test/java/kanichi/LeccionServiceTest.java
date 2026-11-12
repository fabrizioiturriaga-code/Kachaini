package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.VistaUnidad;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EstudianteRepository;
import kanichi.service.LeccionService;
import kanichi.service.MapaMisionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-12 (energía) y HU-18 (lecciones y desbloqueo de unidades). */
class LeccionServiceTest {
  private static final Leccion PRIMERA = Leccion.U1_PRIMEROS_POBLADORES;
  private static final int CORRECTA = PRIMERA.getPregunta().respuestaCorrecta();
  private static final int INCORRECTA = (CORRECTA + 1) % 3;

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final MapaMisionesService mapa = new MapaMisionesService();
  private Estudiante estudiante;
  private Instant t0;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    estudiante.completarOnboarding();
    t0 = Instant.now();
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
    when(repository.bloquearPorId(1L)).thenReturn(Optional.of(estudiante));
  }

  private LeccionService servicioEn(Instant instante) {
    return new LeccionService(repository, mapa, Clock.fixed(instante, ZoneOffset.UTC));
  }

  @Test
  void respuestaCorrectaOtorgaXpYCompletaLaLeccion() {
    ResultadoLeccion resultado = servicioEn(t0).responder(1L, PRIMERA, CORRECTA);

    assertEquals(Resultado.CORRECTA, resultado.resultado());
    assertEquals(PRIMERA.getXp(), resultado.xpGanada());
    assertFalse(resultado.unidadCompletada());
    assertEquals(PRIMERA.getXp(), estudiante.getXp());
    assertTrue(estudiante.haCompletado(PRIMERA));
    assertEquals(5, resultado.energia().actual());
  }

  @Test
  void respuestaIncorrectaRestaEnergiaYNoOtorgaRecompensa() {
    ResultadoLeccion resultado = servicioEn(t0).responder(1L, PRIMERA, INCORRECTA);

    assertEquals(Resultado.INCORRECTA, resultado.resultado());
    assertEquals(4, resultado.energia().actual());
    assertEquals(0, estudiante.getXp());
    assertFalse(estudiante.haCompletado(PRIMERA));
  }

  @Test
  void sinRespuestaTambienCuentaComoError() {
    ResultadoLeccion resultado = servicioEn(t0).responder(1L, PRIMERA, null);

    assertEquals(Resultado.INCORRECTA, resultado.resultado());
    assertEquals(4, resultado.energia().actual());
  }

  @Test
  void conLaEnergiaAgotadaNoSePuedeAvanzarNiConRespuestaCorrecta() {
    LeccionService servicio = servicioEn(t0);
    for (int i = 0; i < Estudiante.ENERGIA_MAXIMA; i++) {
      servicio.responder(1L, PRIMERA, INCORRECTA);
    }

    ResultadoLeccion bloqueado = servicio.responder(1L, PRIMERA, CORRECTA);

    assertEquals(Resultado.SIN_ENERGIA, bloqueado.resultado());
    assertTrue(bloqueado.energia().isAgotada());
    assertEquals(10, bloqueado.energia().minutosParaRecarga());
    assertFalse(estudiante.haCompletado(PRIMERA));
    assertEquals(0, estudiante.getXp());
  }

  @Test
  void laEnergiaSeRecuperaConElTiempoYPermiteContinuar() {
    LeccionService servicio = servicioEn(t0);
    for (int i = 0; i < Estudiante.ENERGIA_MAXIMA; i++) {
      servicio.responder(1L, PRIMERA, INCORRECTA);
    }

    ResultadoLeccion despues =
        servicioEn(t0.plus(Duration.ofMinutes(11))).responder(1L, PRIMERA, CORRECTA);

    assertEquals(Resultado.CORRECTA, despues.resultado());
    assertTrue(estudiante.haCompletado(PRIMERA));
  }

  @Test
  void noSePuedeSaltarUnaLeccion() {
    assertThrows(
        MisionNoPermitidaException.class,
        () -> servicioEn(t0).responder(1L, Leccion.U1_CAZADORES_DEL_PERU, 0));
    assertEquals(5, estudiante.estadoEnergia(t0).actual());
  }

  @Test
  void noSePuedeEstudiarSinCompletarElModuloInicial() {
    Estudiante nuevo = new Estudiante("Beto", "beto@example.org", "hash", 1, "A");
    when(repository.bloquearPorId(2L)).thenReturn(Optional.of(nuevo));

    assertThrows(
        MisionNoPermitidaException.class, () -> servicioEn(t0).responder(2L, PRIMERA, CORRECTA));
  }

  @Test
  void repetirUnaLeccionNoDuplicaLaXpNiRestaEnergia() {
    LeccionService servicio = servicioEn(t0);
    servicio.responder(1L, PRIMERA, CORRECTA);

    ResultadoLeccion repetida = servicio.responder(1L, PRIMERA, INCORRECTA);

    assertEquals(Resultado.YA_COMPLETADA, repetida.resultado());
    assertEquals(0, repetida.xpGanada());
    assertEquals(PRIMERA.getXp(), estudiante.getXp());
    assertEquals(5, repetida.energia().actual());
  }

  @Test
  void completarLaUltimaLeccionDeUnaUnidadDesbloqueaLaSiguiente() {
    LeccionService servicio = servicioEn(t0);
    ResultadoLeccion ultimo = null;
    for (Leccion leccion : Leccion.deUnidad(1)) {
      ultimo = servicio.responder(1L, leccion, leccion.getPregunta().respuestaCorrecta());
    }

    assertTrue(ultimo.unidadCompletada());
    assertEquals(2, ultimo.unidadDesbloqueada());
    var unidades = servicio.obtenerMapa(1L).unidades();
    assertTrue(unidades.get(0).isCompletada());
    assertTrue(unidades.get(1).isActiva());
  }

  @Test
  void alTerminarLaUltimaUnidadNoHayNuevoNivel() {
    LeccionService servicio = servicioEn(t0);
    ResultadoLeccion ultimo = null;
    for (Leccion leccion : Leccion.values()) {
      ultimo = servicio.responder(1L, leccion, leccion.getPregunta().respuestaCorrecta());
    }

    assertTrue(ultimo.unidadCompletada());
    assertNull(ultimo.unidadDesbloqueada());
  }

  @Test
  void elDetalleDeUnidadMarcaLasLeccionesDisponiblesYBloqueadas() {
    servicioEn(t0).responder(1L, PRIMERA, CORRECTA);

    VistaUnidad vista = servicioEn(t0).obtenerUnidad(1L, 1);

    assertTrue(vista.lecciones().get(0).completada());
    assertTrue(vista.lecciones().get(1).disponible());
    assertFalse(vista.lecciones().get(2).disponible());
    assertEquals(1, vista.unidad().leccionesCompletadas());
  }
}
