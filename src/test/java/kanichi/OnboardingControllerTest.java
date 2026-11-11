package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.OnboardingController;
import kanichi.dto.ProgresoModulo;
import kanichi.dto.ProgresoModulo.EstadoMision;
import kanichi.dto.ResultadoMision;
import kanichi.dto.UsuarioSesion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Logro;
import kanichi.model.MisionInicial;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.MapaMisionesService;
import kanichi.service.ModuloInicialService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** HU-06: acceso al módulo inicial con contenido mínimo. */
@WebMvcTest(OnboardingController.class)
@Import({
  SecurityConfig.class,
  UsuarioDetailsService.class,
  LimiteIntentosLogin.class,
  MapaMisionesService.class
})
class OnboardingControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ModuloInicialService moduloInicialService;

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

    when(moduloInicialService.obtenerProgreso(1L)).thenReturn(progresoInicial());
  }

  @Test
  void estudianteNuevoVeElModuloInicialConSusMisiones() throws Exception {
    mvc.perform(get("/onboarding").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/onboarding"))
        .andExpect(content().string(containsString("Módulo inicial")))
        .andExpect(content().string(containsString("href=\"/onboarding/misiones/bienvenida\"")))
        .andExpect(content().string(containsString("🔒 Bloqueada")))
        .andExpect(content().string(containsString("aria-label=\"Navegación principal\"")));
  }

  @Test
  void muestraElTutorial() throws Exception {
    mvc.perform(get("/onboarding/misiones/bienvenida").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/mision"))
        .andExpect(content().string(containsString("Tutorial")))
        .andExpect(content().string(containsString("barra inferior")))
        .andExpect(content().string(containsString("Completar misión")));
  }

  @Test
  void misionBloqueadaRedirigeAlModulo() throws Exception {
    mvc.perform(get("/onboarding/misiones/linea-de-tiempo").with(user(estudiante())))
        .andExpect(redirectedUrl("/onboarding"))
        .andExpect(flash().attribute("error", "Completa primero la misión anterior."));
  }

  @Test
  void misionInexistenteDevuelve404() throws Exception {
    mvc.perform(get("/onboarding/misiones/no-existe").with(user(estudiante())))
        .andExpect(status().isNotFound());
  }

  @Test
  void completarMisionMuestraLaRecompensa() throws Exception {
    when(moduloInicialService.completar(1L, MisionInicial.BIENVENIDA, null))
        .thenReturn(new ResultadoMision(10, List.of(Logro.PRIMEROS_PASOS), false));

    mvc.perform(post("/onboarding/misiones/bienvenida").with(csrf()).with(user(estudiante())))
        .andExpect(redirectedUrl("/onboarding"))
        .andExpect(
            flash()
                .attribute(
                    "mensaje",
                    "¡Misión completada! Ganaste 10 XP y la insignia 👣 Primeros pasos."));
  }

  @Test
  void respuestaIncorrectaVuelveALaMisionConError() throws Exception {
    when(moduloInicialService.completar(eq(1L), eq(MisionInicial.RETO_DE_BIENVENIDA), any()))
        .thenThrow(new MisionNoPermitidaException("Respuesta incorrecta."));

    mvc.perform(
            post("/onboarding/misiones/reto-de-bienvenida")
                .param("respuesta", "0")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/mision"))
        .andExpect(content().string(containsString("Respuesta incorrecta.")));
  }

  @Test
  void completarElModuloDesbloqueaInicioYActualizaLaSesion() throws Exception {
    when(moduloInicialService.completar(1L, MisionInicial.RETO_DE_BIENVENIDA, 1))
        .thenReturn(
            new ResultadoMision(30, List.of(Logro.PRIMER_ACIERTO, Logro.EXPLORADOR), true));
    UsuarioSesion sesion = estudiante();

    mvc.perform(
            post("/onboarding/misiones/reto-de-bienvenida")
                .param("respuesta", "1")
                .with(csrf())
                .with(user(sesion)))
        .andExpect(redirectedUrl("/inicio"))
        .andExpect(
            flash()
                .attribute(
                    "mensaje",
                    containsString("se desbloqueó la unidad 1: Poblamiento de América y del Perú")));

    assertTrue(sesion.isOnboardingCompletado());
    assertEquals("/inicio", sesion.destino());
  }

  @Test
  void postSinCsrfEsRechazado() throws Exception {
    mvc.perform(post("/onboarding/misiones/bienvenida").with(user(estudiante())))
        .andExpect(status().isForbidden());
    verify(moduloInicialService, never()).completar(any(), any(), any());
  }

  @Test
  void docenteNoAccedeAlModuloInicial() throws Exception {
    var docente =
        new UsuarioSesion(2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
    mvc.perform(get("/onboarding").with(user(docente))).andExpect(status().isForbidden());
    mvc.perform(get("/onboarding/misiones/bienvenida").with(user(docente)))
        .andExpect(status().isForbidden());
  }

  private static UsuarioSesion estudiante() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);
  }

  private static ProgresoModulo progresoInicial() {
    var misiones =
        Arrays.stream(MisionInicial.values())
            .map(m -> new EstadoMision(m, false, m.ordinal() == 0))
            .toList();
    return new ProgresoModulo(misiones, 0, List.of(), false);
  }
}
