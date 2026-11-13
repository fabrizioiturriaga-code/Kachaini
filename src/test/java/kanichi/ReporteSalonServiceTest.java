package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import kanichi.dto.ReporteSalon;
import kanichi.dto.ReporteSalon.FilaReporte;
import kanichi.dto.ReporteSalon.UnidadReporte;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.ReporteSalonRepository;
import kanichi.repository.ReporteSalonRepository.IntentosPorEstudiante;
import kanichi.repository.ReporteSalonRepository.XpPorEstudiante;
import kanichi.service.MapaMisionesService;
import kanichi.service.ReporteSalonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReporteSalonServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");
  private static final Instant INICIO_SEMANA = Instant.parse("2026-11-06T05:00:00Z");
  private static final Instant INICIO_ANTERIOR = Instant.parse("2026-10-30T05:00:00Z");
  private static final Instant HOY = Instant.parse("2026-11-12T05:00:00Z");
  private static final Instant MANANA = Instant.parse("2026-11-13T05:00:00Z");

  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final ReporteSalonRepository reportes = mock(ReporteSalonRepository.class);
  private final ReporteSalonService servicio =
      new ReporteSalonService(
          estudiantes, reportes, new MapaMisionesService(), Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private Estudiante beto;

  @BeforeEach
  void setup() {
    ana = estudiante(1L, "Ana", 1, "A");
    ana.completarOnboarding();
    ana.registrarLeccion(Leccion.U1_PRIMEROS_POBLADORES, AHORA.minusSeconds(3600));
    ana.registrarLeccion(Leccion.U1_CAZADORES_DEL_PERU, AHORA.minusSeconds(1800));
    ana.registrarLeccion(Leccion.U1_AGRICULTURA, AHORA.minusSeconds(600));
    beto = estudiante(2L, "Beto", 1, "B");

    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(anyInt(), anyString()))
        .thenReturn(List.of());
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "A")).thenReturn(List.of(ana));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "B")).thenReturn(List.of(beto));
    List<XpPorEstudiante> xpSemanal = List.of(xp(1L, 60));
    List<IntentosPorEstudiante> intentos = List.of(intentos(1L, 4, 0), intentos(2L, 2, 6));
    when(reportes.xpPorEstudianteEntre(anyCollection(), eq(INICIO_SEMANA), eq(MANANA)))
        .thenReturn(xpSemanal);
    when(reportes.intentosPorEstudiante(anyCollection())).thenReturn(intentos);
    when(reportes.sumarXpEntre(anyCollection(), any(), any())).thenReturn(0);
    when(reportes.sumarXpEntre(anyCollection(), eq(INICIO_ANTERIOR), eq(INICIO_SEMANA)))
        .thenReturn(30);
    when(reportes.sumarXpEntre(anyCollection(), eq(HOY), eq(MANANA))).thenReturn(60);
  }

  @Test
  void elReporteResumeElRendimientoDelGradoSinMezclarGrados() {
    ReporteSalon reporte = servicio.generar(1, null);

    assertEquals("1.° de secundaria, todas las secciones", reporte.salon());
    assertEquals(
        List.of("Ana", "Beto"), reporte.filas().stream().map(FilaReporte::nombre).toList());
    verify(estudiantes, never()).findByGradoAndSeccionOrderByNombreAsc(eq(2), anyString());
    assertEquals(25, reporte.promedioAvance());
    assertEquals(60, reporte.xpTotal());
    assertEquals(60, reporte.xpSemana());
    assertEquals(30, reporte.xpSemanaAnterior());
    assertEquals(100, reporte.variacionSemanal());
    assertEquals(1, reporte.activosSemana());
    assertEquals(50, reporte.porcentajeActivos());
    assertEquals(50, reporte.porcentajeAciertos());
    assertEquals(7, reporte.semana().size());
    assertEquals(100, reporte.semana().get(6).porcentaje());
  }

  @Test
  void elReporteMuestraElAvancePorUnidad() {
    List<UnidadReporte> unidades = servicio.generar(1, null).unidades();

    assertEquals(List.of(1, 2), unidades.stream().map(UnidadReporte::numero).toList());
    assertEquals(1, unidades.get(0).completaron());
    assertEquals(1, unidades.get(0).sinEmpezar());
    assertEquals(50, unidades.get(0).porcentaje());
    assertEquals(0, unidades.get(1).completaron());
  }

  @Test
  void destacaALosActivosYSenalaAQuienesNecesitanApoyo() {
    ReporteSalon reporte = servicio.generar(1, null);

    assertEquals(List.of("Ana"), reporte.destacados().stream().map(FilaReporte::nombre).toList());
    assertEquals(
        List.of("Beto"), reporte.requierenApoyo().stream().map(FilaReporte::nombre).toList());
    FilaReporte beto = reporte.requierenApoyo().get(0);
    assertTrue(beto.motivoApoyo().contains("Sin actividad en los últimos 7 días"));
    assertTrue(beto.motivoApoyo().contains("Aciertos por debajo del 60 %"));
    assertEquals(25, beto.porcentajeAciertos());
    assertTrue(
        reporte
            .observaciones()
            .contains("1 de 2 estudiantes ganaron XP en los últimos 7 días (50 %)."));
    assertTrue(
        reporte
            .observaciones()
            .contains("La XP del salón subió 100 % respecto a los 7 días anteriores."));
  }

  @Test
  void conPocosIntentosNoSeMarcaPorAciertos() {
    List<IntentosPorEstudiante> pocos = List.of(intentos(1L, 0, 3));
    when(reportes.intentosPorEstudiante(anyCollection())).thenReturn(pocos);

    FilaReporte ana = servicio.generar(1, "A").filas().get(0);

    assertFalse(ana.necesitaApoyo());
    assertEquals(0, ana.porcentajeAciertos());
  }

  @Test
  void unaSeccionSoloIncluyeASusEstudiantes() {
    ReporteSalon reporte = servicio.generar(1, "b");

    assertEquals("1.° B", reporte.salon());
    assertEquals(List.of("Beto"), reporte.filas().stream().map(FilaReporte::nombre).toList());
    verify(estudiantes, never()).findByGradoAndSeccionOrderByNombreAsc(1, "A");
  }

  @Test
  void unSalonVacioNoConsultaLaActividad() {
    ReporteSalon reporte = servicio.generar(3, "A");

    assertTrue(reporte.isVacio());
    assertTrue(reporte.observaciones().isEmpty());
    verifyNoInteractions(reportes);
  }

  @Test
  void elGradoDebeSerValido() {
    assertThrows(IllegalArgumentException.class, () -> servicio.generar(6, null));
  }

  @Test
  void elCsvTieneElResumenYUnaFilaPorEstudiante() {
    ReporteSalon reporte = servicio.generar(1, null);

    String csv = servicio.exportarCsv(reporte);

    assertTrue(
        csv.startsWith(
            "\uFEFFReporte de rendimiento;1.° de secundaria, todas las secciones\r\n"));
    assertTrue(csv.contains("Estudiante;Sección;Avance del grado (%)"));
    assertTrue(csv.contains("Ana;A;50;3;60;60;1;4;0;100;\r\n"));
    assertTrue(csv.contains("Beto;B;0;0;0;0;0;2;6;25;Sin actividad"));
    assertEquals("reporte-1-todas-2026-11-12.csv", servicio.nombreArchivo(reporte));
    assertEquals("reporte-1B-2026-11-12.csv", servicio.nombreArchivo(servicio.generar(1, "B")));
  }

  @Test
  void elCsvEscapaLosValoresPeligrosos() {
    Estudiante raro = estudiante(3L, "=HIPERVINCULO(\"x\";\"y\")", 1, "C");
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(1, "C")).thenReturn(List.of(raro));

    String csv = servicio.exportarCsv(servicio.generar(1, "C"));

    assertTrue(csv.contains("\"'=HIPERVINCULO(\"\"x\"\";\"\"y\"\")\";C;"));
  }

  private static Estudiante estudiante(Long id, String nombre, int grado, String seccion) {
    Estudiante estudiante = new Estudiante(nombre, id + "@example.org", "hash", grado, seccion);
    ReflectionTestUtils.setField(estudiante, "id", id);
    return estudiante;
  }

  private static XpPorEstudiante xp(Long id, int puntos) {
    XpPorEstudiante xp = mock(XpPorEstudiante.class);
    when(xp.getId()).thenReturn(id);
    when(xp.getPuntos()).thenReturn(puntos);
    return xp;
  }

  private static IntentosPorEstudiante intentos(Long id, long aciertos, long errores) {
    IntentosPorEstudiante intentos = mock(IntentosPorEstudiante.class);
    when(intentos.getId()).thenReturn(id);
    when(intentos.getAciertos()).thenReturn(aciertos);
    when(intentos.getErrores()).thenReturn(errores);
    return intentos;
  }
}
