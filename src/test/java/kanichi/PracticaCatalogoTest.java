package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kanichi.model.ActividadArrastre;
import kanichi.model.ActividadArrastre.Elemento;
import kanichi.model.Lectura;
import kanichi.model.Lectura.Tema;
import org.junit.jupiter.api.Test;

class PracticaCatalogoTest {
  @Test
  void hayUnaLecturaPorUnidadConCodigoUnico() {
    Set<String> codigos = new HashSet<>();
    Set<Integer> unidades = new HashSet<>();
    for (Lectura lectura : Lectura.values()) {
      assertTrue(codigos.add(lectura.getCodigo()), lectura.name());
      assertTrue(unidades.add(lectura.getUnidad()), lectura.name());
      assertEquals(lectura, Lectura.porCodigo(lectura.getCodigo()).orElseThrow());
    }
    assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), unidades);
    assertTrue(Lectura.porCodigo("no-existe").isEmpty());
  }

  @Test
  void cadaLecturaTieneTextoYPreguntasValidasDeTodosLosTemas() {
    for (Lectura lectura : Lectura.values()) {
      assertFalse(lectura.getParrafos().isEmpty(), lectura.name());
      assertEquals(4, lectura.getPreguntas().size(), lectura.name());
      Set<Tema> temas =
          lectura.getPreguntas().stream().map(p -> p.tema()).collect(Collectors.toSet());
      assertEquals(Set.of(Tema.values()), temas, lectura.name());
      lectura
          .getPreguntas()
          .forEach(
              pregunta -> {
                assertEquals(3, pregunta.opciones().size(), pregunta.enunciado());
                assertTrue(pregunta.acepta(pregunta.respuestaCorrecta()), pregunta.enunciado());
                assertFalse(pregunta.acepta(null));
                assertFalse(pregunta.explicacion().isBlank(), pregunta.enunciado());
                assertTrue(pregunta.opcion(3).isEmpty());
              });
    }
  }

  @Test
  void hayUnaActividadDeArrastrePorUnidadConCodigoUnico() {
    Set<String> codigos = new HashSet<>();
    Set<Integer> unidades = new HashSet<>();
    for (ActividadArrastre actividad : ActividadArrastre.values()) {
      assertTrue(codigos.add(actividad.getCodigo()), actividad.name());
      assertTrue(unidades.add(actividad.getUnidad()), actividad.name());
      assertEquals(actividad, ActividadArrastre.porCodigo(actividad.getCodigo()).orElseThrow());
    }
    assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), unidades);
    assertTrue(ActividadArrastre.porCodigo("no-existe").isEmpty());
  }

  @Test
  void cadaActividadTieneParejasSinRepetir() {
    for (ActividadArrastre actividad : ActividadArrastre.values()) {
      var parejas = actividad.getParejas();
      assertEquals(4, parejas.size(), actividad.name());
      assertEquals(
          parejas.size(),
          parejas.stream().map(p -> p.elemento()).distinct().count(),
          actividad.name());
      assertEquals(
          parejas.size(),
          parejas.stream().map(p -> p.destino()).distinct().count(),
          actividad.name());
    }
  }

  @Test
  void losElementosSeMuestranMezcladosYSiempreEnElMismoOrden() {
    for (ActividadArrastre actividad : ActividadArrastre.values()) {
      List<Elemento> mezclados = actividad.elementosMezclados();
      assertEquals(
          IntStream.range(0, actividad.getParejas().size()).boxed().collect(Collectors.toSet()),
          mezclados.stream().map(Elemento::indice).collect(Collectors.toSet()),
          actividad.name());
      assertFalse(
          IntStream.range(0, mezclados.size()).allMatch(i -> mezclados.get(i).indice() == i),
          actividad.name());
      mezclados.forEach(
          e -> assertEquals(actividad.getParejas().get(e.indice()).elemento(), e.texto()));
      assertEquals(mezclados, actividad.elementosMezclados());
    }
  }
}
