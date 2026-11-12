package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import kanichi.dto.ActividadPractica;
import kanichi.dto.ResultadoLectura;
import kanichi.dto.ResultadoLectura.ResumenTema;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Lectura;
import kanichi.model.Lectura.PreguntaLectura;
import kanichi.repository.EstudianteRepository;
import kanichi.service.LecturaService;
import kanichi.service.MapaMisionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LecturaServiceTest {
  private static final Lectura LECTURA = Lectura.HOMBRE_DE_LAURICOCHA;

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final LecturaService servicio = new LecturaService(repository, new MapaMisionesService());
  private Estudiante estudiante;

  @BeforeEach
  void setup() {
    estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
  }

  @Test
  void todasCorrectasDanUnResumenSinErrores() {
    ResultadoLectura resultado = servicio.corregir(LECTURA, respuestasCorrectas());

    assertEquals(4, resultado.aciertos());
    assertEquals(4, resultado.total());
    assertTrue(resultado.isPerfecto());
    assertTrue(resultado.temas().stream().allMatch(t -> t.errores() == 0));
  }

  @Test
  void elResumenAgrupaAciertosYErroresPorTema() {
    List<Integer> respuestas = respuestasCorrectas();
    respuestas.set(1, incorrecta(LECTURA.getPreguntas().get(1)));
    respuestas.set(3, incorrecta(LECTURA.getPreguntas().get(3)));

    ResultadoLectura resultado = servicio.corregir(LECTURA, respuestas);

    assertEquals(2, resultado.aciertos());
    assertFalse(resultado.isPerfecto());
    assertEquals(
        List.of(
            new ResumenTema("Idea principal", 1, 0),
            new ResumenTema("Datos del texto", 1, 1),
            new ResumenTema("Inferencia", 0, 1)),
        resultado.temas());
    assertFalse(resultado.preguntas().get(1).correcta());
    assertEquals(
        LECTURA.getPreguntas().get(1).opcion(respuestas.get(1)).orElseThrow(),
        resultado.preguntas().get(1).respuestaElegida());
  }

  @Test
  void unaOpcionFueraDeRangoCuentaComoError() {
    List<Integer> respuestas = respuestasCorrectas();
    respuestas.set(0, 7);

    ResultadoLectura resultado = servicio.corregir(LECTURA, respuestas);

    assertFalse(resultado.preguntas().get(0).correcta());
    assertEquals("-", resultado.preguntas().get(0).respuestaElegida());
  }

  @Test
  void noSeCorrigeSiFaltanRespuestas() {
    List<Integer> respuestas = respuestasCorrectas();
    respuestas.set(2, null);

    assertThrows(IllegalArgumentException.class, () -> servicio.corregir(LECTURA, respuestas));
    assertThrows(
        IllegalArgumentException.class, () -> servicio.corregir(LECTURA, List.of(0, 1)));
    assertThrows(IllegalArgumentException.class, () -> servicio.corregir(LECTURA, null));
  }

  @Test
  void sinModuloInicialTodasLasLecturasEstanBloqueadas() {
    List<ActividadPractica> lecturas = servicio.listar(1L);

    assertEquals(2, lecturas.size());
    assertTrue(lecturas.stream().noneMatch(ActividadPractica::disponible));
    assertFalse(servicio.estaDisponible(1L, LECTURA));
  }

  @Test
  void lasLecturasSeDesbloqueanConSuUnidad() {
    estudiante.completarOnboarding();

    assertTrue(servicio.estaDisponible(1L, Lectura.HOMBRE_DE_LAURICOCHA));
    assertFalse(servicio.estaDisponible(1L, Lectura.CARAL_CIUDAD_SAGRADA));

    Leccion.deUnidad(1).forEach(estudiante::registrarLeccion);

    assertTrue(servicio.estaDisponible(1L, Lectura.CARAL_CIUDAD_SAGRADA));
    ActividadPractica caral = servicio.listar(1L).get(1);
    assertEquals("caral-ciudad-sagrada", caral.codigo());
    assertEquals("Culturas preíncas", caral.tituloUnidad());
    assertTrue(caral.disponible());
  }

  @Test
  void soloSeListanLasLecturasDelGradoDelEstudiante() {
    Estudiante deCuarto = new Estudiante("Luis", "luis@example.org", "hash", 4, "B");
    deCuarto.completarOnboarding();
    when(repository.findById(2L)).thenReturn(Optional.of(deCuarto));

    List<ActividadPractica> lecturas = servicio.listar(2L);

    assertEquals(
        List.of("las-islas-del-guano", "el-combate-de-angamos"),
        lecturas.stream().map(ActividadPractica::codigo).toList());
    assertEquals(List.of(true, false), lecturas.stream().map(ActividadPractica::disponible).toList());
    assertThrows(
        ResponseStatusException.class,
        () -> servicio.estaDisponible(2L, Lectura.HOMBRE_DE_LAURICOCHA));
  }

  private static List<Integer> respuestasCorrectas() {
    return new ArrayList<>(
        Arrays.asList(
            LECTURA.getPreguntas().stream().map(PreguntaLectura::respuestaCorrecta).toArray(Integer[]::new)));
  }

  private static int incorrecta(PreguntaLectura pregunta) {
    return (pregunta.respuestaCorrecta() + 1) % pregunta.opciones().size();
  }
}
