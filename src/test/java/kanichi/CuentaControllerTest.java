package kanichi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.CuentaController;
import kanichi.dto.CambioContrasenaForm;
import kanichi.dto.PerfilEstudianteForm;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.CambioContrasenaInvalidoException;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.CambioContrasenaService;
import kanichi.service.PerfilEstudianteService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CuentaController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class CuentaControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean PerfilEstudianteService perfilEstudianteService;
  @MockBean CambioContrasenaService cambioContrasenaService;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;

  @BeforeEach
  void setup() {
    when(perfilEstudianteService.obtener(1L))
        .thenReturn(new Estudiante("Ana", "ana@example.org", "hash", 1, "A"));
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
  void estudiantePuedeConsultarPerfil() throws Exception {
    Estudiante estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(perfilEstudianteService.obtener(1L)).thenReturn(estudiante);

    mvc.perform(get("/estudiante/perfil").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Mi perfil")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Zorro")));
  }

  @Test
  void docenteNoPuedeConsultarPerfilEstudiantil() throws Exception {
    mvc.perform(get("/estudiante/perfil").with(user(docenteSesion())))
        .andExpect(status().isForbidden());
  }

  @Test
  void guardaPerfilValido() throws Exception {
    mvc.perform(
            post("/estudiante/perfil")
                .with(user(estudianteSesion()))
                .with(csrf())
                .param("nombre", "Ana María")
                .param("avatar", "PUMA"))
        .andExpect(redirectedUrl("/estudiante/perfil"))
        .andExpect(flash().attribute("mensaje", "Perfil actualizado."));

    verify(perfilEstudianteService).actualizar(eq(1L), any(PerfilEstudianteForm.class));
  }

  @Test
  void rechazaAvatarFueraDelCatalogo() throws Exception {
    mvc.perform(
            post("/estudiante/perfil")
                .with(user(estudianteSesion()))
                .with(csrf())
                .param("nombre", "Ana")
                .param("avatar", "URL_EXTERNA"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("perfilForm", "avatar"));

    verify(perfilEstudianteService, never()).actualizar(anyLong(), any(PerfilEstudianteForm.class));
  }

  @Test
  void docentePuedeAbrirFormularioDeContrasena() throws Exception {
    mvc.perform(get("/cuenta/contrasena").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Contraseña actual")));
  }

  @Test
  void guardaCambioDeContrasenaValido() throws Exception {
    mvc.perform(
            post("/cuenta/contrasena")
                .with(user(docenteSesion()))
                .with(csrf())
                .param("contrasenaActual", "Historia123")
                .param("contrasenaNueva", "NuevaClave456")
                .param("confirmacion", "NuevaClave456"))
        .andExpect(redirectedUrl("/cuenta/contrasena"))
        .andExpect(flash().attribute("mensaje", "Contraseña actualizada."));

    verify(cambioContrasenaService).cambiar(eq(2L), any(CambioContrasenaForm.class));
  }

  @Test
  void errorDeCambioNoDevuelveContrasenas() throws Exception {
    doThrow(new CambioContrasenaInvalidoException("contrasenaActual", "La actual es incorrecta."))
        .when(cambioContrasenaService)
        .cambiar(eq(1L), any(CambioContrasenaForm.class));

    mvc.perform(
            post("/cuenta/contrasena")
                .with(user(estudianteSesion()))
                .with(csrf())
                .param("contrasenaActual", "SecretoActual123")
                .param("contrasenaNueva", "SecretoNuevo456")
                .param("confirmacion", "SecretoNuevo456"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("cambioContrasenaForm", "contrasenaActual"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SecretoActual123"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SecretoNuevo456"))));
  }

  @Test
  void postSinCsrfNoCambiaContrasena() throws Exception {
    mvc.perform(
            post("/cuenta/contrasena")
                .with(user(estudianteSesion()))
                .param("contrasenaActual", "Historia123")
                .param("contrasenaNueva", "NuevaClave456")
                .param("confirmacion", "NuevaClave456"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(cambioContrasenaService);
  }

  @Test
  void elPerfilMuestraElBotonDeCerrarSesion() throws Exception {
    mvc.perform(get("/estudiante/perfil").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Cerrar sesión")));
  }

  @Test
  void cerrarSesionLlevaAlLogin() throws Exception {
    mvc.perform(post("/logout").with(user(estudianteSesion())).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  void guardarElPerfilActualizaElAvatarDeLaSesion() throws Exception {
    UsuarioSesion sesion = estudianteSesion();

    mvc.perform(
            post("/estudiante/perfil")
                .with(user(sesion))
                .with(csrf())
                .param("nombre", "Ana")
                .param("avatar", "ZORRO"))
        .andExpect(redirectedUrl("/estudiante/perfil"));

    org.junit.jupiter.api.Assertions.assertEquals("🦊", sesion.getAvatar());
  }

  private UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);
  }

  private UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}
