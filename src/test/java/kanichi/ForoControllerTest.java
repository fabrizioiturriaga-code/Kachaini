package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.ForoController;
import kanichi.dto.ForoSalon;
import kanichi.dto.MensajeForo;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.model.TipoPublicacion;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.ForoService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ForoController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class ForoControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ForoService foroService;

  @BeforeEach
  void setup() {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(1L);
    when(estudiante.getRol()).thenReturn(Rol.ESTUDIANTE);
    when(estudiante.isAutorizado()).thenReturn(true);
    when(usuarios.findByCorreo("ana@example.org")).thenReturn(Optional.of(estudiante));

    Docente docente = mock(Docente.class);
    when(docente.getId()).thenReturn(2L);
    when(docente.getRol()).thenReturn(Rol.DOCENTE);
    when(docente.isAutorizado()).thenReturn(true);
    when(usuarios.findByCorreo("docente@example.org")).thenReturn(Optional.of(docente));
  }

  @Test
  void elEstudianteVeElForoDeSuSalon() throws Exception {
    when(foroService.verForo(1L))
        .thenReturn(
            new ForoSalon(
                2,
                "A",
                List.of(
                    new MensajeForo(
                        "Ana", "✨", "Hola salón", "13/11 10:00", TipoPublicacion.MENSAJE, true),
                    new MensajeForo(
                        "Kaichaini",
                        "🤖",
                        "🎉 ¡El salón 2.° A completó el reto «Energía Inca»!",
                        "13/11 09:00",
                        TipoPublicacion.RETO_SALON,
                        false))));

    mvc.perform(get("/estudiante/foro").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/foro"))
        .andExpect(content().string(containsString("Solo lo ven los estudiantes de 2.° A.")))
        .andExpect(content().string(containsString("Hola salón")))
        .andExpect(content().string(containsString("(Tú)")))
        .andExpect(content().string(containsString("foro-automatico")))
        .andExpect(content().string(containsString("Kaichaini")))
        .andExpect(content().string(containsString("maxlength=\"500\"")));
  }

  @Test
  void unForoVacioInvitaAEscribir() throws Exception {
    when(foroService.verForo(1L)).thenReturn(new ForoSalon(2, "A", List.of()));

    mvc.perform(get("/estudiante/foro").with(user(estudianteSesion())))
        .andExpect(content().string(containsString("Todavía no hay mensajes")));
  }

  @Test
  void publicarUnMensajeVuelveAlForo() throws Exception {
    mvc.perform(
            post("/estudiante/foro")
                .param("texto", "¿Quién repasa conmigo?")
                .with(csrf())
                .with(user(estudianteSesion())))
        .andExpect(redirectedUrl("/estudiante/foro"))
        .andExpect(flash().attribute("mensaje", "Mensaje publicado."));
    verify(foroService).publicar(1L, "¿Quién repasa conmigo?");
  }

  @Test
  void unMensajeInvalidoMuestraElError() throws Exception {
    doThrow(new IllegalArgumentException("Escribe un mensaje antes de publicar."))
        .when(foroService)
        .publicar(1L, " ");

    mvc.perform(
            post("/estudiante/foro")
                .param("texto", " ")
                .with(csrf())
                .with(user(estudianteSesion())))
        .andExpect(redirectedUrl("/estudiante/foro"))
        .andExpect(flash().attribute("error", "Escribe un mensaje antes de publicar."));
  }

  @Test
  void soloLosEstudiantesEntranAlForo() throws Exception {
    mvc.perform(get("/estudiante/foro").with(user(docenteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(post("/estudiante/foro").param("texto", "Hola").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/foro"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    verifyNoInteractions(foroService);
  }

  private UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }

  private UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}