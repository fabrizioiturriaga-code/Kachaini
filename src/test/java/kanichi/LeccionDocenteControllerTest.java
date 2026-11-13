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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kanichi.config.SecurityConfig;
import kanichi.controller.LeccionDocenteController;
import kanichi.dto.EstadoEnergia;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadesDocente.EnMapa;
import kanichi.dto.UnidadesDocente.EstadoLeccion;
import kanichi.dto.UnidadesDocente.VistaLeccion;
import kanichi.dto.UnidadesDocente.VistaUnidad;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Estudiante;
import kanichi.model.LeccionDocente;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.LeccionDocenteService;
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

@WebMvcTest(LeccionDocenteController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class LeccionDocenteControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean LeccionDocenteService leccionDocenteService;

  private final EstadoEnergia energia = new EstadoEnergia(5, 5, 0);
  private EnMapa unidad;
  private LeccionDocente leccion;

  @BeforeEach
  void setup() {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(1L);
    when(estudiante.getRol()).thenReturn(Rol.ESTUDIANTE);
    when(estudiante.isAutorizado()).thenReturn(true);
    when(usuarios.findByCorreo("ana@example.org")).thenReturn(Optional.of(estudiante));

    unidad = new EnMapa(7L, new UnidadMapa(3, "La República", UnidadMapa.Estado.ACTIVA, 0, 1));
    leccion =
        new LeccionDocente(
            7L, 1, UnidadDocenteService.validar(UnidadDocenteServiceTest.formulario()));
    ReflectionTestUtils.setField(leccion, "id", 30L);
    when(leccionDocenteService.obtenerLeccion(1L, 30L))
        .thenReturn(new VistaLeccion(leccion, unidad, false, true, energia));
  }

  @Test
  void laUnidadMuestraSusLecciones() throws Exception {
    when(leccionDocenteService.obtenerUnidad(1L, 7L))
        .thenReturn(
            new VistaUnidad(unidad, List.of(new EstadoLeccion(leccion, false, true)), energia));

    mvc.perform(get("/estudiante/unidades-docente/7").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Unidad 3: La República")))
        .andExpect(content().string(containsString("href=\"/estudiante/lecciones-docente/30\"")));
  }

  @Test
  void laLeccionMuestraContenidoYPreguntaConTresOpciones() throws Exception {
    mvc.perform(get("/estudiante/lecciones-docente/30").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Primer párrafo.")))
        .andExpect(content().string(containsString("¿Quiénes construyeron Machu Picchu?")))
        .andExpect(content().string(containsString("Azteca")))
        .andExpect(content().string(containsString("name=\"intento\"")));
  }

  @Test
  void respuestaCorrectaMuestraLaXpGanada() throws Exception {
    when(leccionDocenteService.responder(eq(1L), eq(30L), eq(0), any()))
        .thenReturn(
            new ResultadoLeccion(
                ResultadoLeccion.Resultado.CORRECTA, 20, energia, true, null, 0, false));

    mvc.perform(
            post("/estudiante/lecciones-docente/30")
                .param("respuesta", "0")
                .param("intento", UUID.randomUUID().toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Correcto! Ganaste 20 XP.")))
        .andExpect(content().string(containsString("¡Completaste la unidad «La República»!")))
        .andExpect(content().string(containsString("Continuar al mapa")));
  }

  @Test
  void respuestaIncorrectaInformaLaEnergia() throws Exception {
    when(leccionDocenteService.responder(eq(1L), eq(30L), eq(2), any()))
        .thenReturn(
            new ResultadoLeccion(
                ResultadoLeccion.Resultado.INCORRECTA,
                0,
                new EstadoEnergia(4, 5, 600),
                false,
                null,
                0,
                false));

    mvc.perform(
            post("/estudiante/lecciones-docente/30")
                .param("respuesta", "2")
                .param("intento", UUID.randomUUID().toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("te quedan 4 de 5")));
  }

  @Test
  void sinModuloInicialRedirigeAlOnboarding() throws Exception {
    var pendiente =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);
    mvc.perform(get("/estudiante/lecciones-docente/30").with(user(pendiente)))
        .andExpect(redirectedUrl("/onboarding"));
  }

  private static UsuarioSesion estudiante() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }
}
