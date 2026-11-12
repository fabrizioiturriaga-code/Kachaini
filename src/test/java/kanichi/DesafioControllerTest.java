package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.DesafioController;
import kanichi.dto.PanelDesafios;
import kanichi.dto.PanelDesafios.Companero;
import kanichi.dto.PanelDesafios.Participante;
import kanichi.dto.PanelDesafios.VistaDesafio;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.DesafioInvalidoException;
import kanichi.model.Desafio;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.model.TipoDesafio;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.DesafioService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DesafioController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class DesafioControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean DesafioService desafioService;

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
  void muestraBarraDeProgresoParticipantesYEstado() throws Exception {
    VistaDesafio activo =
        new VistaDesafio(
            5L,
            TipoDesafio.DUO_EN_MARCHA,
            Desafio.Estado.ACTIVO,
            List.of(
                new Participante("Ana", "✨", 2, 100, true),
                new Participante("Luis", "🦊", 1, 75, false)),
            3,
            88,
            false);
    VistaDesafio pendiente =
        new VistaDesafio(
            6L,
            TipoDesafio.EQUIPO_EXPERTO,
            Desafio.Estado.PENDIENTE,
            List.of(
                new Participante("Ana", "✨", 0, 0, true),
                new Participante("Rosa", "🐦", 0, 0, false)),
            0,
            0,
            false);
    when(desafioService.panel(1L))
        .thenReturn(
            new PanelDesafios(
                true,
                List.of(new Companero(2L, "Luis", "🦊")),
                List.of(),
                List.of(activo, pendiente),
                List.of()));

    mvc.perform(get("/estudiante/desafios").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/desafios"))
        .andExpect(content().string(containsString("Desafíos entre estudiantes")))
        .andExpect(content().string(containsString("<progress")))
        .andExpect(content().string(containsString("3 de 4 tareas")))
        .andExpect(content().string(containsString("88 % de aciertos")))
        .andExpect(content().string(containsString("Luis")))
        .andExpect(content().string(containsString("(Tú)")))
        .andExpect(content().string(containsString(">Activo<")))
        .andExpect(content().string(containsString(">Pendiente<")))
        .andExpect(content().string(containsString("Enviar invitación")));
  }

  @Test
  void sinActividadNoMuestraElFormulario() throws Exception {
    when(desafioService.panel(1L))
        .thenReturn(new PanelDesafios(false, List.of(), List.of(), List.of(), List.of()));

    mvc.perform(get("/estudiante/desafios").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Completa al menos una lección o práctica")))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Enviar invitación"))));
  }

  @Test
  void crearRedirigeConConfirmacion() throws Exception {
    mvc.perform(
            post("/estudiante/desafios")
                .param("companeroId", "2")
                .param("tipo", "DUO_EN_MARCHA")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(redirectedUrl("/estudiante/desafios"))
        .andExpect(flash().attributeExists("mensaje"));
    verify(desafioService).crear(1L, 2L, null, TipoDesafio.DUO_EN_MARCHA);
  }

  @Test
  void errorAlCrearSeMuestraComoMensaje() throws Exception {
    doThrow(new DesafioInvalidoException("No puedes desafiarte a ti mismo."))
        .when(desafioService)
        .crear(any(), any(), any(), any());

    mvc.perform(
            post("/estudiante/desafios")
                .param("companeroId", "1")
                .param("tipo", "DUO_EN_MARCHA")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(redirectedUrl("/estudiante/desafios"))
        .andExpect(flash().attribute("error", "No puedes desafiarte a ti mismo."));
  }

  @Test
  void aceptarInvitacion() throws Exception {
    mvc.perform(post("/estudiante/desafios/5/aceptar").with(csrf()).with(user(estudiante())))
        .andExpect(redirectedUrl("/estudiante/desafios"));
    verify(desafioService).aceptar(1L, 5L);
  }

  @Test
  void postSinCsrfEsRechazado() throws Exception {
    mvc.perform(post("/estudiante/desafios").param("tipo", "DUO_EN_MARCHA").with(user(estudiante())))
        .andExpect(status().isForbidden());
    verify(desafioService, never()).crear(any(), any(), any(), any());
  }

  @Test
  void docenteNoAccede() throws Exception {
    var docente =
        new UsuarioSesion(2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
    mvc.perform(get("/estudiante/desafios").with(user(docente))).andExpect(status().isForbidden());
  }

  private static UsuarioSesion estudiante() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }
}
