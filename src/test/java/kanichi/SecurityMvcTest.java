package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.*;
import kanichi.dto.UsuarioSesion;
import kanichi.model.*;
import kanichi.repository.UsuarioRepository;
import kanichi.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@org.junit.jupiter.api.extension.ExtendWith(
    org.springframework.boot.test.system.OutputCaptureExtension.class)
@WebMvcTest({AuthController.class, IntegracionController.class, OnboardingController.class})
@Import({
  SecurityConfig.class,
  UsuarioDetailsService.class,
  MapaMisionesService.class,
  kanichi.security.LimiteIntentosLogin.class
})
class SecurityMvcTest {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder encoder;
  @MockBean UsuarioRepository usuarios;
  @MockBean AuthService auth;
  @MockBean ModuloInicialService moduloInicial;

  @MockBean kanichi.security.LimiteIntentosLogin limite;

  @BeforeEach
  void setup() {
    when(moduloInicial.obtenerProgreso(any()))
        .thenReturn(
            new kanichi.dto.ProgresoModulo(java.util.List.of(), 0, java.util.List.of(), false));
    when(usuarios.findByCorreo("ana@example.org"))
        .thenReturn(
            Optional.of(
                new Estudiante("Ana", "ana@example.org", encoder.encode("Historia123"), 1, "A")));
  }

  @Test
  void formularios() throws Exception {
    mvc.perform(get("/registro"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    mvc.perform(get("/login")).andExpect(status().isOk());
  }

  @Test
  void accesoAnonimo() throws Exception {
    mvc.perform(get("/inicio"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void csrfObligatorio() throws Exception {
    mvc.perform(post("/login")).andExpect(status().isForbidden());
    mvc.perform(post("/registro")).andExpect(status().isForbidden());
    mvc.perform(post("/logout")).andExpect(status().isForbidden());
  }

  @Test
  void loginSesionYLogout() throws Exception {
    MockHttpSession previa = new MockHttpSession();
    String idAnterior = previa.getId();
    var result =
        mvc.perform(
                post("/login")
                    .session(previa)
                    .with(csrf())
                    .param("correo", "ANA@EXAMPLE.ORG")
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/onboarding"))
            .andReturn();
    var sesion = (MockHttpSession) result.getRequest().getSession(false);
    assertNotEquals(idAnterior, sesion.getId());
    var context = (SecurityContext) sesion.getAttribute("SPRING_SECURITY_CONTEXT");
    var principal = (UsuarioSesion) context.getAuthentication().getPrincipal();
    assertNull(principal.getPassword());
    assertNull(context.getAuthentication().getCredentials());
    mvc.perform(get("/onboarding").session(sesion)).andExpect(status().isOk());
    mvc.perform(get("/docente/panel").session(sesion)).andExpect(status().isForbidden());
    mvc.perform(post("/logout").session(sesion).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"));
    assertTrue(sesion.isInvalid());
  }

  @Test
  void registroCorrecto() throws Exception {
    mvc.perform(
            post("/registro")
                .with(csrf())
                .param("nombre", "Ana")
                .param("correo", "ana@example.org")
                .param("contrasena", "Historia123")
                .param("confirmacion", "Historia123")
                .param("rol", "ESTUDIANTE")
                .param("grado", "1")
                .param("seccion", "A"))
        .andExpect(redirectedUrl("/login"))
        .andExpect(flash().attributeExists("mensaje"));
    verify(auth).registrar(any());
  }

  @Test
  void errorRegistroNoDevuelveContrasena() throws Exception {
    doThrow(
            new kanichi.exception.RegistroInvalidoException(
                java.util.Map.of("confirmacion", "No coincide")))
        .when(auth)
        .registrar(any());
    mvc.perform(
            post("/registro")
                .with(csrf())
                .param("nombre", "Ana")
                .param("correo", "ana@example.org")
                .param("contrasena", "Secreto123")
                .param("confirmacion", "Otra1234")
                .param("rol", "ESTUDIANTE"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("registroForm", "confirmacion"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Secreto123"))));
  }

  @Test
  void rolManipulado() throws Exception {
    mvc.perform(
            post("/registro")
                .with(csrf())
                .param("nombre", "Ana")
                .param("correo", "ana@example.org")
                .param("rol", "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("registroForm", "rol"));
    verifyNoInteractions(auth);
  }

  @Test
  void formatoLoginInvalido() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("correo", "invalido")
                .param("contrasena", "Historia123"))
        .andExpect(redirectedUrl("/login?error"));
    verify(usuarios, never()).findByCorreo("invalido");
  }

  @Test
  void incorrecta() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("correo", "ana@example.org")
                .param("contrasena", "mala"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void inexistente() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("correo", "nadie@example.org")
                .param("contrasena", "Historia123"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void pendienteNoIngresa() throws Exception {
    when(usuarios.findByCorreo("d@example.org"))
        .thenReturn(
            Optional.of(
                new Docente("D", "d@example.org", encoder.encode("Historia123"), "Historia")));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("correo", "d@example.org")
                .param("contrasena", "Historia123"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void docenteAutorizado() throws Exception {
    Docente docente = mock(Docente.class);
    when(docente.getCorreo()).thenReturn("d@example.org");
    when(docente.getNombre()).thenReturn("Docente");
    when(docente.getRol()).thenReturn(Rol.DOCENTE);
    when(docente.isAutorizado()).thenReturn(true);
    when(docente.getContrasenaHash()).thenReturn(encoder.encode("Historia123"));
    when(usuarios.findByCorreo("d@example.org")).thenReturn(Optional.of(docente));
    var result =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("correo", "d@example.org")
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/docente/panel"))
            .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    mvc.perform(get("/docente/panel").session(session)).andExpect(status().isOk());
    mvc.perform(get("/inicio").session(session)).andExpect(status().isForbidden());
  }

  @Test
  void devuelveEsperaSinConsultarCredenciales() throws Exception {
    when(limite.segundosRestantes("127.0.0.1")).thenReturn(30L);
    mvc.perform(
            post("/login")
                .servletPath("/login")
                .with(csrf())
                .param("correo", "ana@example.org")
                .param("contrasena", "Historia123"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "30"));
    verify(usuarios, never()).findByCorreo(anyString());
  }

  @Test
  void erroresTienenMensajesYAsociacionesAccesibles() throws Exception {
    mvc.perform(
            post("/registro")
                .with(csrf())
                .param("nombre", "")
                .param("correo", "invalido")
                .param("rol", "ADMIN")
                .param("grado", "texto"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Completa este campo.")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("Selecciona Estudiante o Docente.")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("aria-describedby=\"error-nombre\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-invalid=\"true\"")));
  }

  @Test
  void eventosNoIncluyenCredenciales(org.springframework.boot.test.system.CapturedOutput salida)
      throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("correo", "ana@example.org")
                .param("contrasena", "SecretoNoRegistrar123"))
        .andExpect(redirectedUrl("/login?error"));
    assertTrue(salida.getOut().contains("evento=LOGIN_FALLIDO"));
    assertFalse(salida.getOut().contains("SecretoNoRegistrar123"));
    assertFalse(salida.getOut().contains("ana@example.org"));
  }
}
