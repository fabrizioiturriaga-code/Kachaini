package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.service.EvaluadorLogros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-13: requisitos de los logros del mapa de aprendizaje. */
class EvaluadorLogrosTest {
  private static final Instant HOY = Instant.parse("2026-11-12T18:00:00Z");
  private Estudiante estudiante;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    estudiante.completarOnboarding();
  }

  @Test
  void sinLeccionesNoSeOtorgaNingunLogro() {
    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).isEmpty());
  }

  @Test
  void laPrimeraLeccionOtorgaAprendizDeHistoria() {
    estudiante.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, HOY);

    assertEquals(
        List.of(Logro.APRENDIZ_DE_HISTORIA), EvaluadorLogros.otorgarNuevos(estudiante, HOY));
    assertTrue(estudiante.tieneLogro(Logro.APRENDIZ_DE_HISTORIA));
  }

  @Test
  void unLogroSeOtorgaUnaSolaVez() {
    estudiante.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, HOY);
    EvaluadorLogros.otorgarNuevos(estudiante, HOY);
    estudiante.registrarLeccion(Leccion.U1_CAZADORES_DEL_PERU, HOY);

    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).isEmpty());
  }

  @Test
  void completarUnaUnidadOtorgaUnidadDominada() {
    completar(Leccion.deUnidad(1), HOY);

    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).contains(Logro.UNIDAD_DOMINADA));
  }

  @Test
  void alcanzar100XpOtorgaCienPuntos() {
    for (MisionInicial mision : MisionInicial.values()) {
      estudiante.registrarMision(mision);
    }
    estudiante.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, HOY);
    assertTrue(estudiante.getXp() >= Logro.XP_PARA_CIEN_XP);

    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).contains(Logro.CIEN_XP));
  }

  @Test
  void tresDiasSeguidosOtorgaConstancia() {
    estudiante.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, HOY.minus(Duration.ofDays(2)));
    estudiante.registrarLeccion(Leccion.U1_CAZADORES_DEL_PERU, HOY.minus(Duration.ofDays(1)));
    estudiante.registrarLeccion(Leccion.U1_AGRICULTURA, HOY);

    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).contains(Logro.RACHA_DE_TRES));
  }

  @Test
  void completarTodoElGradoOtorgaMaestroDelGrado() {
    for (int unidad : Leccion.unidadesDelGrado(1)) {
      completar(Leccion.deUnidad(unidad), HOY);
    }

    assertTrue(EvaluadorLogros.otorgarNuevos(estudiante, HOY).contains(Logro.MAESTRO_DEL_GRADO));
  }

  @Test
  void todosLosLogrosExplicanComoConseguirlos() {
    for (Logro logro : Logro.values()) {
      assertFalse(logro.getComoConseguirlo().isBlank(), logro.name());
    }
  }

  private void completar(List<Leccion> lecciones, Instant fecha) {
    lecciones.forEach(leccion -> estudiante.registrarLeccion(leccion, fecha));
  }
}