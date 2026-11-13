package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.UnidadDocenteController;
import kanichi.dto.UnidadesDocente.Resumen;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.LeccionDocente;
import kanichi.model.Rol;
import kanichi.model.UnidadDocente;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.UnidadDocenteService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UnidadDocenteController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class UnidadDocenteControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean UnidadDocenteService unidadDocenteService;

  private UnidadDocente unidad;
  private LeccionDocente leccion;

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

    unidad = new UnidadDocente(2L, 3, "La República", Instant.parse("2026-11-13T15:00:00Z"));
    ReflectionTestUtils.setField(unidad, "id", 7L);
    leccion =
        new LeccionDocente(
            7L, 1, UnidadDocenteService.validar(UnidadDocenteServiceTest.formulario()));
    ReflectionTestUtils.setField(leccion, "id", 30L);
  }

  @Test
  void misUnidadesMuestraLaListaYElFormulario() throws Exception {
    when(unidadDocenteService.deDocente(2L)).thenReturn(List.of(new Resumen(unidad, 1)));

    mvc.perform(get("/docente/unidades").with(user(docente())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/unidades"))
        .andExpect(content().string(containsString("Mis unidades")))
        .andExpect(content().string(containsString("La República")))
        .andExpect(content().string(containsString("3.° de secundaria · 1 lección")))
        .andExpect(content().string(containsString("name=\"respuestaCorrecta\"")))
        .andExpect(content().string(containsString("Crear unidad")));
  }

  @Test
  void crearUnidadRedirigeASuPagina() throws Exception {
    when(unidadDocenteService.crear(eq(2L), eq(3), eq("La República"), any())).thenReturn(7L);

    mvc.perform(
            post("/docente/unidades")
                .param("grado", "3")
                .param("tituloUnidad", "La República")
                .param("titulo", "Los incas")
                .param("contenido", "Texto")
                .param("pregunta", "¿Pregunta?")
                .param("opcionA", "A")
                .param("opcionB", "B")
                .param("opcionC", "C")
                .param("respuestaCorrecta", "1")
                .with(csrf())
                .with(user(docente())))
        .andExpect(redirectedUrl("/docente/unidades/7"))
        .andExpect(flash().attributeExists("mensaje"));
    verify(unidadDocenteService)
        .crear(
            eq(2L),
            eq(3),
            eq("La República"),
            argThat(f -> "Los incas".equals(f.getTitulo()) && f.getRespuestaCorrecta() == 1));
  }

  @Test
  void errorAlCrearConservaLosDatos() throws Exception {
    when(unidadDocenteService.crear(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Escribe la pregunta."));

    mvc.perform(
            post("/docente/unidades")
                .param("grado", "3")
                .param("tituloUnidad", "La República")
                .param("titulo", "Los incas")
                .with(csrf())
                .with(user(docente())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Escribe la pregunta.")))
        .andExpect(content().string(containsString("value=\"La República\"")))
        .andExpect(content().string(containsString("value=\"Los incas\"")));
  }

  @Test
  void laUnidadMuestraSusLeccionesParaEditar() throws Exception {
    when(unidadDocenteService.unidad(2L, 7L)).thenReturn(unidad);
    when(unidadDocenteService.lecciones(2L, 7L)).thenReturn(List.of(leccion));

    mvc.perform(get("/docente/unidades/7").with(user(docente())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Visible para los estudiantes de 3.°")))
        .andExpect(content().string(containsString("Respuesta correcta: Inca")))
        .andExpect(content().string(containsString("href=\"/docente/unidades/7/lecciones/30\"")))
        .andExpect(content().string(containsString("Agregar lección")));
  }

  @Test
  void editarLeccionMuestraElFormularioConSusDatos() throws Exception {
    when(unidadDocenteService.unidad(2L, 7L)).thenReturn(unidad);
    when(unidadDocenteService.leccion(2L, 7L, 30L)).thenReturn(leccion);

    mvc.perform(get("/docente/unidades/7/lecciones/30").with(user(docente())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Los incas\"")))
        .andExpect(content().string(containsString("Guardar lección")));
  }

  @Test
  void guardarLeccionRedirigeALaUnidad() throws Exception {
    mvc.perform(
            post("/docente/unidades/7/lecciones/30")
                .param("titulo", "Nuevo")
                .with(csrf())
                .with(user(docente())))
        .andExpect(redirectedUrl("/docente/unidades/7"));
    verify(unidadDocenteService).actualizarLeccion(eq(2L), eq(7L), eq(30L), any());
  }

  @Test
  void elEstudianteNoAccedeAlPanelDeUnidades() throws Exception {
    var estudiante =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
    mvc.perform(get("/docente/unidades").with(user(estudiante)))
        .andExpect(status().isForbidden());
  }

  @Test
  void postSinCsrfEsRechazado() throws Exception {
    mvc.perform(post("/docente/unidades").param("grado", "3").with(user(docente())))
        .andExpect(status().isForbidden());
    verify(unidadDocenteService, never()).crear(any(), any(), any(), any());
  }

  private static UsuarioSesion docente() {
    return new UsuarioSesion(2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}
