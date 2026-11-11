package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.NavegacionController;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.dto.ProgresoModulo;
import kanichi.dto.RankingSalon;
import kanichi.model.Logro;
import kanichi.service.MapaMisionesService;
import kanichi.service.ModuloInicialService;
import kanichi.service.RankingService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** HU-08: barra inferior de navegación y mapa de misiones en Inicio. */
@WebMvcTest(NavegacionController.class)
@Import({
  SecurityConfig.class,
  UsuarioDetailsService.class,
  LimiteIntentosLogin.class,
  MapaMisionesService.class
})
class NavegacionControllerTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ModuloInicialService moduloInicialService;
  @MockBean RankingService rankingService;

  @BeforeEach
  void setup() {
    when(moduloInicialService.obtenerProgreso(1L))
        .thenReturn(new ProgresoModulo(List.of(), 10, List.of(Logro.PRIMEROS_PASOS), false));
    when(rankingService.obtenerRankingDelSalon(1L))
        .thenReturn(
            new RankingSalon(
                1, "A", List.of(new RankingSalon.Posicion(1, "Ana", "✨", 10, true)), 1, 10));

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

  @ParameterizedTest
  @CsvSource({
    "/inicio, estudiante/inicio",
    "/estudiante/retos, estudiante/retos",
    "/estudiante/comunidad, estudiante/comunidad"
  })
  void cadaSeccionMuestraLaBarraInferiorConLosCuatroDestinos(String ruta, String vista)
      throws Exception {
    mvc.perform(get(ruta).with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name(vista))
        .andExpect(content().string(containsString("aria-label=\"Navegación principal\"")))
        .andExpect(content().string(containsString("href=\"/inicio\"")))
        .andExpect(content().string(containsString("href=\"/estudiante/retos\"")))
        .andExpect(content().string(containsString("href=\"/estudiante/comunidad\"")))
        .andExpect(content().string(containsString("href=\"/estudiante/perfil\"")));
  }

  @Test
  void laSeccionActualQuedaMarcada() throws Exception {
    mvc.perform(get("/estudiante/retos").with(user(estudianteSesion())))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<a href=\"/estudiante/retos\""
                            + "(?=[^>]*barra-inferior__enlace--activo)"
                            + "(?=[^>]*aria-current=\"page\")[^>]*>.*")))
        .andExpect(
            content()
                .string(matchesPattern("(?s).*<a href=\"/inicio\"(?![^>]*aria-current)[^>]*>.*")));
  }

  @Test
  void inicioMuestraElMapaDeMisionesConLaUnidadActiva() throws Exception {
    mvc.perform(get("/inicio").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(model().attribute("unidades", hasSize(6)))
        .andExpect(model().attributeExists("unidadActiva"))
        .andExpect(content().string(containsString("Mapa de misiones")))
        .andExpect(content().string(containsString("unidad--activa")))
        .andExpect(content().string(containsString("En progreso")));
  }

  @Test
  void inicioRedirigeAlModuloInicialSiNoLoCompleto() throws Exception {
    var pendiente =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);
    mvc.perform(get("/inicio").with(user(pendiente))).andExpect(redirectedUrl("/onboarding"));
  }

  @Test
  void retosMuestraLosLogrosYLaXp() throws Exception {
    mvc.perform(get("/estudiante/retos").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Mis logros")))
        .andExpect(content().string(containsString("Primeros pasos")))
        .andExpect(content().string(containsString("(Obtenido)")))
        .andExpect(content().string(containsString("(Por desbloquear)")));
  }

  @Test
  void comunidadMuestraElRankingDelSalon() throws Exception {
    mvc.perform(get("/estudiante/comunidad").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ranking de tu salón")))
        .andExpect(content().string(containsString("10 XP")))
        .andExpect(content().string(containsString("(Tú)")));
  }

  @Test
  void anonimoEsRedirigidoAlLogin() throws Exception {
    mvc.perform(get("/estudiante/retos"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void docenteNoAccedeALaNavegacionDelEstudiante() throws Exception {
    mvc.perform(get("/inicio").with(user(docenteSesion()))).andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/comunidad").with(user(docenteSesion())))
        .andExpect(status().isForbidden());
  }

  private UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }

  private UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}