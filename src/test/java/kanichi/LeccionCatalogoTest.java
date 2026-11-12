package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import kanichi.model.Leccion;
import org.junit.jupiter.api.Test;

/** HU-18: el catálogo de lecciones es coherente con las unidades del mapa. */
class LeccionCatalogoTest {
  @Test
  void cadaUnidadTieneElMismoNumeroDeLecciones() {
    for (int unidad = 1; unidad <= 6; unidad++) {
      assertEquals(Leccion.LECCIONES_POR_UNIDAD, Leccion.deUnidad(unidad).size(), "unidad " + unidad);
    }
    assertEquals(6 * Leccion.LECCIONES_POR_UNIDAD, Leccion.values().length);
  }

  @Test
  void losCodigosSonUnicosYSeEncuentranPorCodigo() {
    Set<String> codigos = new HashSet<>();
    for (Leccion leccion : Leccion.values()) {
      assertTrue(codigos.add(leccion.getCodigo()), "código repetido: " + leccion.getCodigo());
      assertEquals(leccion, Leccion.porCodigo(leccion.getCodigo()).orElseThrow());
    }
    assertTrue(Leccion.porCodigo("no-existe").isEmpty());
  }

  @Test
  void lasLeccionesDeUnaUnidadSonConsecutivas() {
    Leccion[] todas = Leccion.values();
    for (int i = 1; i < todas.length; i++) {
      assertTrue(todas[i].getUnidad() >= todas[i - 1].getUnidad(), todas[i].name());
    }
  }

  @Test
  void cadaPreguntaTieneUnaRespuestaCorrectaValidaYUnaExplicacion() {
    Arrays.stream(Leccion.values())
        .forEach(
            leccion -> {
              var pregunta = leccion.getPregunta();
              assertEquals(3, pregunta.opciones().size(), leccion.name());
              assertTrue(
                  pregunta.respuestaCorrecta() >= 0
                      && pregunta.respuestaCorrecta() < pregunta.opciones().size(),
                  leccion.name());
              assertFalse(pregunta.explicacion().isBlank(), leccion.name());
              assertFalse(leccion.getContenido().isEmpty(), leccion.name());
              assertTrue(leccion.getXp() > 0, leccion.name());
              assertTrue(leccion.aceptaRespuesta(pregunta.respuestaCorrecta()));
              assertFalse(leccion.aceptaRespuesta(null));
            });
  }

  @Test
  void laPrimeraLeccionNoTieneAnteriorYLasDemasSi() {
    assertTrue(Leccion.values()[0].anterior().isEmpty());
    assertEquals(Leccion.values()[0], Leccion.values()[1].anterior().orElseThrow());
  }
}
