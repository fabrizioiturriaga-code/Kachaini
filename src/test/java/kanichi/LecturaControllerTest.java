package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import kanichi.controller.LecturaController;
import kanichi.dto.ActividadPractica;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Lectura;
import kanichi.model.Rol;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.LecturaService;
import kanichi.service.MapaMisionesService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LecturaController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class LecturaControllerTest {
  private static final Lectura LECTURA = Lectura.HOMBRE_DE_LAURICOCHA;
  private static final String RUTA = "/estudiante/lecturas/" + LECTURA.getCodigo();

  private final LecturaService corrector =
      new LecturaService(mock(EstudianteRepository.class), new MapaMisionesService());

  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean LecturaService lecturaService;

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

    when(lecturaService.estaDisponible(1L, LECTURA)).thenReturn(true);
    when(lecturaService.corregir(any(), any()))
        .thenAnswer(i -> corrector.corregir(i.getArgument(0), i.getArgument(1)));
  }

  @Test
  void laListaMuestraLasLecturasDisponiblesYBloqueadas() throws Exception {
    when(lecturaService.listar(1L))
        .thenReturn(
            List.of(
                new ActividadPractica(
                    "hombre-de-lauricocha", "El hombre de Lauricocha", 1, "Poblamiento", true),
                new ActividadPractica(
                    "caral-ciudad-sagrada", "Caral, ciudad sagrada", 2, "Culturas", false)));

    mvc.perform(get("/estudiante/lecturas").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/practica"))
        .andExpect(
            content().string(containsString("href=\"/estudiante/lecturas/hombre-de-lauricocha\"")))
        .andExpect(
            content()
                .string(not(containsString("href=\"/estudiante/lecturas/caral-ciudad-sagrada\""))))
        .andExpect(content().string(containsString("🔒 Bloqueada")));
  }

  @Test
  void laLecturaMuestraElTextoYSusPreguntasEnLaMismaPantalla() throws Exception {
    mvc.perform(get(RUTA).with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/lectura"))
        .andExpect(content().string(containsString("Augusto Cardich exploró unas cuevas")))
        .andExpect(content().string(containsString("¿De qué trata principalmente el texto?")))
        .andExpect(content().string(containsString("name=\"respuestas[3]\"")))
        .andExpect(content().string(containsString("Escuchar el texto")))
        .andExpect(content().string(containsString("Enviar respuestas")));
  }

  @Test
  void alEnviarSeMuestraElResumenPorTema() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "1")
                .param("respuestas[1]", "0")
                .param("respuestas[2]", "0")
                .param("respuestas[3]", "0")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Acertaste 3 de 4 preguntas.")))
        .andExpect(content().string(containsString("Aciertos y errores por tema")))
        .andExpect(content().string(containsString("Datos del texto")))
        .andExpect(content().string(containsString("❌ Incorrecta")))
        .andExpect(content().string(containsString(LECTURA.getPreguntas().get(2).explicacion())))
        .andExpect(content().string(containsString("Volver a intentarlo")))
        .andExpect(content().string(not(containsString("Enviar respuestas"))));
  }

  @Test
  void todasCorrectasFelicitaAlEstudiante() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "1")
                .param("respuestas[1]", "0")
                .param("respuestas[2]", "2")
                .param("respuestas[3]", "0")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(content().string(containsString("Acertaste 4 de 4 preguntas.")))
        .andExpect(content().string(containsString("¡Excelente! Comprendiste todo el texto.")));
  }

  @Test
  void siFaltanRespuestasPideCompletarlasYConservaLasElegidas() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "1")
                .param("respuestas[2]", "2")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Responde todas las preguntas antes de enviar.")))
        .andExpect(content().string(containsString("Enviar respuestas")))
        .andExpect(content().string(containsString("checked")));
    verify(lecturaService, never()).corregir(any(), any());
  }

  @Test
  void unValorNoNumericoTambienPideCompletarLasRespuestas() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "abc")
                .param("respuestas[1]", "0")
                .param("respuestas[2]", "0")
                .param("respuestas[3]", "0")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Responde todas las preguntas antes de enviar.")));
  }

  @Test
  void unaLecturaBloqueadaVuelveALaLista() throws Exception {
    when(lecturaService.estaDisponible(1L, Lectura.CARAL_CIUDAD_SAGRADA)).thenReturn(false);

    mvc.perform(get("/estudiante/lecturas/caral-ciudad-sagrada").with(user(estudiante())))
        .andExpect(redirectedUrl("/estudiante/lecturas"))
        .andExpect(flash().attribute("error", "Completa primero la unidad anterior."));
  }

  @Test
  void unaLecturaInexistenteDevuelve404() throws Exception {
    mvc.perform(get("/estudiante/lecturas/no-existe").with(user(estudiante())))
        .andExpect(status().isNotFound());
  }

  @Test
  void elEnvioSinCsrfEsRechazado() throws Exception {
    mvc.perform(post(RUTA).param("respuestas[0]", "1").with(user(estudiante())))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonimoVaAlLoginYDocenteNoAccede() throws Exception {
    mvc.perform(get(RUTA))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    mvc.perform(get("/estudiante/lecturas").with(user(docente())))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinCompletarElModuloInicialVaAlOnboarding() throws Exception {
    var pendiente =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);

    mvc.perform(get("/estudiante/lecturas").with(user(pendiente)))
        .andExpect(redirectedUrl("/onboarding"));
    mvc.perform(get(RUTA).with(user(pendiente))).andExpect(redirectedUrl("/onboarding"));
  }

  private UsuarioSesion estudiante() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }

  private UsuarioSesion docente() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}
