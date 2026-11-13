package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import kanichi.controller.MensajeController;
import kanichi.dto.Conversacion;
import kanichi.dto.Conversacion.Contacto;
import kanichi.dto.Conversacion.MensajeChat;
import kanichi.dto.Conversacion.Resumen;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.MensajeriaService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MensajeController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class MensajeControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean MensajeriaService mensajeriaService;

  private static final Contacto ANA = new Contacto(1L, "Ana", "🦊", "1.° A");
  private static final Contacto LUZ = new Contacto(2L, "Luz", "🧑‍🏫", "Historia");

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
  }

  @Test
  void cadaRolSoloEntraASuBandeja() throws Exception {
    mvc.perform(get("/docente/mensajes").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/mensajes").with(user(docenteSesion())))
        .andExpect(status().isForbidden());
  }

  @Test
  void elDocenteVeSusConversacionesYElContadorDeNoLeidos() throws Exception {
    when(mensajeriaService.bandejaDocente(2L))
        .thenReturn(List.of(new Resumen(ANA, "Tengo una duda", "13/11 10:00", false, 2)));
    when(mensajeriaService.sinLeerDocente(2L)).thenReturn(2);

    mvc.perform(get("/docente/mensajes").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/mensajes"))
        .andExpect(model().attributeDoesNotExist("estudiantes"))
        .andExpect(content().string(containsString("Tengo una duda")))
        .andExpect(content().string(containsString("href=\"/docente/mensajes/1\"")))
        .andExpect(content().string(containsString("aria-label=\"2 sin leer\"")))
        .andExpect(content().string(containsString("href=\"/docente/reportes\"")));
  }

  @Test
  void alElegirUnSalonSeListanSusEstudiantes() throws Exception {
    when(mensajeriaService.estudiantesDelSalon(1, "A")).thenReturn(List.of(ANA));

    mvc.perform(
            get("/docente/mensajes")
                .param("grado", "1")
                .param("seccion", "a")
                .with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"estudiantes\" value=\"1\"")))
        .andExpect(content().string(containsString("Enviar a todo el salón (1 estudiante)")));
  }

  @Test
  void elDocenteEnviaUnMensajeATodoElSalon() throws Exception {
    when(mensajeriaService.enviarAEstudiantes(2L, 1, "A", null, true, "Repasen"))
        .thenReturn(25);

    mvc.perform(
            post("/docente/mensajes")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("grado", "1")
                .param("seccion", "A")
                .param("todos", "true")
                .param("texto", "Repasen"))
        .andExpect(redirectedUrl("/docente/mensajes?grado=1&seccion=A"))
        .andExpect(flash().attribute("mensaje", "Mensaje enviado a 25 estudiantes."));
  }

  @Test
  void losErroresDelEnvioSeMuestranYConservanElTexto() throws Exception {
    when(mensajeriaService.enviarAEstudiantes(eq(2L), eq(1), any(), anyList(), eq(false), any()))
        .thenThrow(
            new IllegalArgumentException("Solo puedes elegir estudiantes del grado seleccionado."));

    mvc.perform(
            post("/docente/mensajes")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("grado", "1")
                .param("estudiantes", "1", "9")
                .param("texto", "Hola"))
        .andExpect(redirectedUrl("/docente/mensajes?grado=1"))
        .andExpect(
            flash().attribute("error", "Solo puedes elegir estudiantes del grado seleccionado."))
        .andExpect(flash().attribute("texto", "Hola"));
  }

  @Test
  void sinCsrfNoSeEnvia() throws Exception {
    mvc.perform(
            post("/docente/mensajes")
                .with(user(docenteSesion()))
                .param("grado", "1")
                .param("todos", "true")
                .param("texto", "Hola"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/estudiante/mensajes/2").with(user(estudianteSesion())).param("texto", "Hola"))
        .andExpect(status().isForbidden());
    verify(mensajeriaService, never())
        .enviarAEstudiantes(any(), anyInt(), any(), any(), anyBoolean(), any());
    verify(mensajeriaService, never()).enviarComoEstudiante(any(), any(), any());
  }

  @Test
  void elDocenteVeYRespondeUnaConversacion() throws Exception {
    when(mensajeriaService.conversacionDocente(2L, 1L))
        .thenReturn(
            new Conversacion(
                ANA,
                List.of(
                    new MensajeChat("Hola Ana", "13/11 09:00", true, true),
                    new MensajeChat("Hola profe", "13/11 09:05", false, false))));

    mvc.perform(get("/docente/mensajes/1").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/conversacion"))
        .andExpect(content().string(containsString("Hola profe")))
        .andExpect(content().string(containsString("· Leído")))
        .andExpect(content().string(containsString("action=\"/docente/mensajes/1\"")));

    mvc.perform(
            post("/docente/mensajes/1")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("texto", "Claro, te explico"))
        .andExpect(redirectedUrl("/docente/mensajes/1"));
    verify(mensajeriaService).responderComoDocente(2L, 1L, "Claro, te explico");
  }

  @Test
  void elEstudianteVeSuBandejaYPuedeEscribirAUnDocente() throws Exception {
    when(mensajeriaService.bandejaEstudiante(1L))
        .thenReturn(List.of(new Resumen(LUZ, "Repasen la unidad", "13/11 08:00", false, 1)));
    when(mensajeriaService.docentesDisponibles()).thenReturn(List.of(LUZ));
    when(mensajeriaService.sinLeerEstudiante(1L)).thenReturn(1);

    mvc.perform(get("/estudiante/mensajes").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/mensajes"))
        .andExpect(content().string(containsString("Repasen la unidad")))
        .andExpect(content().string(containsString("Luz · Historia")))
        .andExpect(content().string(containsString("href=\"/estudiante/mensajes/2\"")))
        .andExpect(content().string(containsString("aria-label=\"1 sin leer\"")))
        .andExpect(content().string(containsString("href=\"/estudiante/avisos\"")));
  }

  @Test
  void elEstudianteEnviaUnMensajeYVaALaConversacion() throws Exception {
    mvc.perform(
            post("/estudiante/mensajes")
                .with(user(estudianteSesion()))
                .with(csrf())
                .param("docente", "2")
                .param("texto", "Tengo una duda"))
        .andExpect(redirectedUrl("/estudiante/mensajes/2"))
        .andExpect(flash().attribute("mensaje", "Mensaje enviado."));
    verify(mensajeriaService).enviarComoEstudiante(1L, 2L, "Tengo una duda");
  }

  @Test
  void siElEnvioDelEstudianteFallaVuelveALaBandeja() throws Exception {
    doThrow(new IllegalArgumentException("Elige un docente."))
        .when(mensajeriaService)
        .enviarComoEstudiante(eq(1L), any(), anyString());

    mvc.perform(
            post("/estudiante/mensajes")
                .with(user(estudianteSesion()))
                .with(csrf())
                .param("texto", "Hola"))
        .andExpect(redirectedUrl("/estudiante/mensajes"))
        .andExpect(flash().attribute("error", "Elige un docente."));
  }

  @Test
  void elEstudianteVeLaConversacionConSuDocente() throws Exception {
    when(mensajeriaService.conversacionEstudiante(1L, 2L))
        .thenReturn(
            new Conversacion(
                LUZ, List.of(new MensajeChat("Repasen la unidad", "13/11 08:00", false, true))));

    mvc.perform(get("/estudiante/mensajes/2").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/conversacion"))
        .andExpect(content().string(containsString("Repasen la unidad")))
        .andExpect(content().string(not(containsString("· Leído"))));
    verify(mensajeriaService, never()).conversacionDocente(anyLong(), anyLong());
  }

  private static UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Luz", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }

  private static UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }
}
