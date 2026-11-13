package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.DocenteController;
import kanichi.dto.DetalleProgresoAlumno;
import kanichi.dto.DetalleProgresoAlumno.AvanceDiario;
import kanichi.dto.ProgresoAlumno;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.FactorXp;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.MapaMisionesService;
import kanichi.service.MultiplicadorXpService;
import kanichi.service.ProgresoDocenteService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocenteController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class DocenteControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ProgresoDocenteService progresoDocenteService;
  @MockBean MultiplicadorXpService multiplicadorXpService;

  private static final ProgresoAlumno ANA =
      new ProgresoAlumno(1L, "Ana", "🦊", "A", 50, 4, 3, 140, 2, "2x", "13/11/2026 10:00");
  private static final ProgresoAlumno BETO =
      new ProgresoAlumno(2L, "Beto", "✨", "B", 0, 1, 0, 10, 0, null, null);

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

    when(progresoDocenteService.listar(eq(1), any())).thenReturn(List.of(ANA, BETO));
  }

  @Test
  void elEstudianteNoAccedeAlPanelDocente() throws Exception {
    mvc.perform(get("/docente/progreso").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/docente/multiplicadores").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinGradoSoloSeMuestraElFiltro() throws Exception {
    mvc.perform(get("/docente/progreso").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/progreso"))
        .andExpect(model().attributeDoesNotExist("estudiantes"))
        .andExpect(content().string(containsString("Ver estudiantes")));
    verifyNoInteractions(progresoDocenteService);
  }

  @Test
  void laListaMuestraLosIndicadoresDelGrado() throws Exception {
    mvc.perform(get("/docente/progreso").param("grado", "1").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ana")))
        .andExpect(content().string(containsString("50 %")))
        .andExpect(content().string(containsString("140")))
        .andExpect(content().string(containsString("2 días")))
        .andExpect(content().string(containsString("href=\"/docente/progreso/1\"")));
    verify(progresoDocenteService).listar(1, null);
  }

  @Test
  void unGradoInvalidoMuestraUnError() throws Exception {
    mvc.perform(get("/docente/progreso").param("grado", "9").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(model().attribute("error", "Selecciona un grado de 1 a 5."))
        .andExpect(model().attributeDoesNotExist("estudiantes"));
  }

  @Test
  void elDetalleMuestraElGraficoSemanal() throws Exception {
    when(progresoDocenteService.detalle(1L))
        .thenReturn(
            new DetalleProgresoAlumno(
                ANA,
                1,
                new MapaMisionesService().obtenerMapa(1, true),
                List.of(new AvanceDiario("jue 12/11", 60, 100))));

    mvc.perform(get("/docente/progreso/1").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/estudiante"))
        .andExpect(content().string(containsString("Avance semanal")))
        .andExpect(content().string(containsString("jue 12/11")))
        .andExpect(content().string(containsString("height: 100%")))
        .andExpect(content().string(containsString("2x hasta 13/11/2026 10:00")));
  }

  @Test
  void laPaginaDeMultiplicadoresListaElSalon() throws Exception {
    mvc.perform(
            get("/docente/multiplicadores")
                .param("grado", "1")
                .param("seccion", "a")
                .with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "A"))
        .andExpect(content().string(containsString("name=\"estudiantes\"")))
        .andExpect(content().string(containsString("1.5x")))
        .andExpect(content().string(containsString("Quitar multiplicador")));
    verify(progresoDocenteService).listar(1, "A");
  }

  @Test
  void asignarUnMultiplicador() throws Exception {
    when(multiplicadorXpService.asignar(2L, 1, List.of(1L, 2L), FactorXp.X2, 7)).thenReturn(2);

    mvc.perform(
            post("/docente/multiplicadores")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("grado", "1")
                .param("seccion", "A")
                .param("estudiantes", "1", "2")
                .param("factor", "X2")
                .param("dias", "7")
                .param("accion", "asignar"))
        .andExpect(redirectedUrl("/docente/multiplicadores?grado=1&seccion=A"))
        .andExpect(
            flash().attribute("mensaje", "Multiplicador 2x asignado a 2 estudiantes por 7 días."));
  }

  @Test
  void quitarUnMultiplicador() throws Exception {
    when(multiplicadorXpService.retirar(1, List.of(1L))).thenReturn(1);

    mvc.perform(
            post("/docente/multiplicadores")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("grado", "1")
                .param("estudiantes", "1")
                .param("accion", "retirar"))
        .andExpect(redirectedUrl("/docente/multiplicadores?grado=1"))
        .andExpect(flash().attribute("mensaje", "Se retiró el multiplicador a 1 estudiante."));
    verify(multiplicadorXpService, never()).asignar(any(), any(Integer.class), any(), any(), any());
  }

  @Test
  void losErroresDeValidacionSeMuestranAlDocente() throws Exception {
    when(multiplicadorXpService.asignar(eq(2L), eq(1), anyList(), any(), any()))
        .thenThrow(
            new IllegalArgumentException("Solo puedes elegir estudiantes del grado seleccionado."));

    mvc.perform(
            post("/docente/multiplicadores")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("grado", "1")
                .param("estudiantes", "1", "5")
                .param("factor", "X3")
                .param("dias", "3"))
        .andExpect(redirectedUrl("/docente/multiplicadores?grado=1"))
        .andExpect(
            flash().attribute("error", "Solo puedes elegir estudiantes del grado seleccionado."));
  }

  @Test
  void sinCsrfNoSeGuarda() throws Exception {
    mvc.perform(
            post("/docente/multiplicadores")
                .with(user(docenteSesion()))
                .param("grado", "1")
                .param("estudiantes", "1")
                .param("factor", "X2")
                .param("dias", "7"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(multiplicadorXpService);
  }

  @Test
  void laNavegacionDelDocenteNoMuestraEnlacesDeEstudiante() throws Exception {
    mvc.perform(get("/docente/progreso").with(user(docenteSesion())))
        .andExpect(content().string(containsString("aria-label=\"Navegación del docente\"")))
        .andExpect(content().string(not(containsString("href=\"/estudiante/retos\""))));
  }

  private static UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Luz", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }

  private static UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }
}
