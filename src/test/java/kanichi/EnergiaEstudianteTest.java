package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import kanichi.dto.EstadoEnergia;
import kanichi.model.Estudiante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-12: la energía baja con los errores, se agota en cero y se recupera con el tiempo. */
class EnergiaEstudianteTest {
  private Estudiante estudiante;
  private Instant t0;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    t0 = Instant.now();
  }

  @Test
  void unEstudianteNuevoTieneLaEnergiaLlena() {
    EstadoEnergia energia = estudiante.estadoEnergia(t0);

    assertEquals(Estudiante.ENERGIA_MAXIMA, energia.actual());
    assertFalse(energia.isRecargando());
    assertFalse(energia.isAgotada());
  }

  @Test
  void cadaErrorRestaUnaUnidadYArrancaLaRecarga() {
    assertTrue(estudiante.perderEnergia(t0));
    assertTrue(estudiante.perderEnergia(t0));

    EstadoEnergia energia = estudiante.estadoEnergia(t0);
    assertEquals(3, energia.actual());
    assertTrue(energia.isRecargando());
    assertEquals(Estudiante.RECARGA_POR_UNIDAD.getSeconds(), energia.segundosParaRecarga());
    assertEquals(10, energia.minutosParaRecarga());
  }

  @Test
  void conLaEnergiaEnCeroNoSePuedePerderMas() {
    for (int i = 0; i < Estudiante.ENERGIA_MAXIMA; i++) {
      assertTrue(estudiante.perderEnergia(t0));
    }

    assertFalse(estudiante.perderEnergia(t0));
    assertTrue(estudiante.estadoEnergia(t0).isAgotada());
    assertEquals(0, estudiante.estadoEnergia(t0).actual());
  }

  @Test
  void recuperaUnaUnidadCadaDiezMinutos() {
    estudiante.perderEnergia(t0);
    estudiante.perderEnergia(t0);

    EstadoEnergia alos14 = estudiante.estadoEnergia(t0.plus(Duration.ofMinutes(14)));
    assertEquals(4, alos14.actual());
    assertEquals(Duration.ofMinutes(6).getSeconds(), alos14.segundosParaRecarga());

    assertEquals(5, estudiante.estadoEnergia(t0.plus(Duration.ofMinutes(20))).actual());
  }

  @Test
  void laEnergiaNuncaSuperaElMaximo() {
    estudiante.perderEnergia(t0);

    EstadoEnergia energia = estudiante.estadoEnergia(t0.plus(Duration.ofHours(5)));

    assertEquals(Estudiante.ENERGIA_MAXIMA, energia.actual());
    assertFalse(energia.isRecargando());
  }

  @Test
  void consultarLaEnergiaNoModificaElEstado() {
    estudiante.perderEnergia(t0);

    estudiante.estadoEnergia(t0.plus(Duration.ofHours(1)));

    assertEquals(4, estudiante.estadoEnergia(t0).actual());
  }

  @Test
  void recargarConservaElTiempoYaTranscurrido() {
    for (int i = 0; i < 3; i++) {
      estudiante.perderEnergia(t0);
    }

    estudiante.recargarEnergia(t0.plus(Duration.ofMinutes(25)));

    EstadoEnergia energia = estudiante.estadoEnergia(t0.plus(Duration.ofMinutes(25)));
    assertEquals(4, energia.actual());
    assertEquals(Duration.ofMinutes(5).getSeconds(), energia.segundosParaRecarga());
  }

  @Test
  void unRelojQueRetrocedeNoRegalaEnergia() {
    estudiante.perderEnergia(t0);

    assertEquals(4, estudiante.estadoEnergia(t0.minus(Duration.ofHours(1))).actual());
  }
}
