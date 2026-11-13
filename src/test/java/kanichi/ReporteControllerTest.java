package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.ReporteController;
import kanichi.dto.DetalleProgresoAlumno.AvanceDiario;
import kanichi.dto.ReporteSalon;
import kanichi.dto.ReporteSalon.FilaReporte;
import kanichi.dto.ReporteSalon.UnidadReporte;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.ReporteSalonService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReporteController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class ReporteControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ReporteSalonService reporteSalonService;

  private static final FilaReporte ANA =
      new FilaReporte(1L, "Ana", "🦊", "A", 50, 3, 120, 60, 2, 8, 2, null);
  private static final FilaReporte BETO =
      new FilaReporte(
          2L, "Beto", "✨", "A", 0, 0, 0, 0, 0, 1, 5, "Sin actividad en los últimos 7 días.");
  private static final ReporteSalon REPORTE =
      new ReporteSalon(
          1,
          "A",
          "13/11/2026 10:00",
          25,
          120,
          60,
          40,
          1,
          1,
          56,
          List.of(new AvanceDiario("jue 12/11", 60, 100)),
          List.of(new UnidadReporte(1, "Los primeros pobladores", 1, 0, 1)),
          List.of(ANA, BETO),
          List.of(ANA),
          List.of(BETO),
          List.of("La XP del salón subió 50 % respecto a los 7 días anteriores."));

  @BeforeEach
  void setup() {
    Docente docente = mock(Docente.class);
    when(docente.getId()).thenReturn(2L);
    when(docente.getRol()).thenReturn(Rol.DOCENTE);
    when(docente.isAutorizado()).thenReturn(true);
    when(usuarios.findByCorreo("docente@example.org")).thenReturn(Optional.of(docente));

    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(1L);
    when(estudiante.getRol()).thenReturn(Rol.ESTUDIANTE);
    when(estudiante.isAutorizado()).thenReturn(true);
    when(usuarios.findByCorreo("ana@example.org")).thenReturn(Optional.of(estudiante));

    when(reporteSalonService.generar(1, "A")).thenReturn(REPORTE);
  }

  @Test
  void elEstudianteNoAccedeALosReportes() throws Exception {
    mvc.perform(get("/docente/reportes").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/docente/reportes/exportar").param("grado", "1").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinGradoSoloSeMuestraElFiltro() throws Exception {
    mvc.perform(get("/docente/reportes").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/reportes"))
        .andExpect(model().attributeDoesNotExist("reporte"))
        .andExpect(content().string(containsString("Generar reporte")))
        .andExpect(content().string(containsString("aria-current=\"page\"")));
    verifyNoInteractions(reporteSalonService);
  }

  @Test
  void elReporteMuestraIndicadoresTendenciasYSeguimiento() throws Exception {
    mvc.perform(
            get("/docente/reportes")
                .param("grado", "1")
                .param("seccion", "a")
                .with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Rendimiento de 1.° A")))
        .andExpect(content().string(containsString("Generado el 13/11/2026 10:00")))
        .andExpect(content().string(containsString("+50 %")))
        .andExpect(content().string(containsString("56 %")))
        .andExpect(content().string(containsString("subió 50 %")))
        .andExpect(content().string(containsString("Los primeros pobladores")))
        .andExpect(content().string(containsString("Destacados de la semana")))
        .andExpect(content().string(containsString("Sin actividad en los últimos 7 días.")))
        .andExpect(content().string(containsString("80 % (8/10)")))
        .andExpect(
            content().string(containsString("/docente/reportes/exportar?grado=1&amp;seccion=A")))
        .andExpect(content().string(containsString("Imprimir o guardar PDF")));
    verify(reporteSalonService).generar(1, "A");
  }

  @Test
  void unGradoInvalidoMuestraUnError() throws Exception {
    mvc.perform(get("/docente/reportes").param("grado", "7").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(model().attribute("error", "Selecciona un grado de 1 a 5."))
        .andExpect(model().attributeDoesNotExist("reporte"));
    verifyNoInteractions(reporteSalonService);
  }

  @Test
  void unSalonVacioNoOfreceExportar() throws Exception {
    ReporteSalon vacio =
        new ReporteSalon(
            3, null, "13/11/2026 10:00", 0, 0, 0, 0, 0, 0, null, List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of());
    when(reporteSalonService.generar(3, null)).thenReturn(vacio);

    mvc.perform(get("/docente/reportes").param("grado", "3").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("No hay estudiantes registrados en este salón.")))
        .andExpect(content().string(not(containsString("Exportar a Excel"))));
  }

  @Test
  void exportarDescargaUnCsv() throws Exception {
    when(reporteSalonService.exportarCsv(REPORTE)).thenReturn("\uFEFFEstudiante;Sección\r\n");
    when(reporteSalonService.nombreArchivo(REPORTE)).thenReturn("reporte-1A-2026-11-13.csv");

    mvc.perform(
            get("/docente/reportes/exportar")
                .param("grado", "1")
                .param("seccion", "A")
                .with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv;charset=UTF-8"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"reporte-1A-2026-11-13.csv\""))
        .andExpect(content().string("\uFEFFEstudiante;Sección\r\n"));
  }

  @Test
  void exportarSinGradoValidoEsUnaPeticionIncorrecta() throws Exception {
    mvc.perform(get("/docente/reportes/exportar").param("grado", "0").with(user(docenteSesion())))
        .andExpect(status().isBadRequest());
    verify(reporteSalonService, never()).generar(anyInt(), any());
  }

  private static UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Luz", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }

  private static UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }
}
