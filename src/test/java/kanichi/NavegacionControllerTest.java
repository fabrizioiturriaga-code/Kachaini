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
import kanichi.dto.EstadoEnergia;
import kanichi.dto.MapaAprendizaje;
import kanichi.dto.ProgresoModulo;
import kanichi.dto.RankingSalon;
import kanichi.model.Logro;
import kanichi.service.LeccionService;
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
  @MockBean LeccionService leccionService;
  @MockBean kanichi.service.LigaService ligaService;
  @MockBean kanichi.service.LeccionDocenteService leccionDocenteService;

  @BeforeEach
  void setup() {
    when(leccionService.obtenerMapa(1L))
        .thenReturn(
            new MapaAprendizaje(
                new MapaMisionesService().obtenerMapa(1, true), new EstadoEnergia(4, 5, 300), 0));
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
        .andExpect(model().attribute("unidades", hasSize(2)))
        .andExpect(model().attributeExists("unidadActiva"))
        .andExpect(content().string(containsString("Mapa de misiones")))
        .andExpect(content().string(containsString("unidad--activa")))
        .andExpect(content().string(containsString("En progreso")));
  }

  @Test
  void inicioMuestraLaEnergiaYElAvanceDeCadaUnidad() throws Exception {
    mvc.perform(get("/inicio").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("energia"))
        .andExpect(content().string(containsString("4/5")))
        .andExpect(content().string(containsString("Próxima recarga en 5 min")))
        .andExpect(content().string(containsString("href=\"/estudiante/unidades/1\"")))
        .andExpect(content().string(containsString("☆☆☆")));
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
        .andExpect(content().string(containsString("Mi colección")))
        .andExpect(content().string(containsString("Primeros pasos")))
        .andExpect(content().string(containsString("insignia--obtenida")))
        .andExpect(content().string(containsString("insignia--bloqueada")))
        .andExpect(content().string(containsString("Cómo conseguirlo:")))
        .andExpect(content().string(containsString(Logro.UNIDAD_DOMINADA.getComoConseguirlo())));
  }

  @Test
  void comunidadMuestraElRankingDelSalon() throws Exception {
    mvc.perform(get("/estudiante/comunidad").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ranking semanal de tu salón")))
        .andExpect(content().string(containsString("10 XP")))
        .andExpect(content().string(containsString("(Tú)")))
        .andExpect(content().string(containsString("Liga Amauta")))
        .andExpect(content().string(containsString("Te faltan 30 XP para subir a Liga Panaca")))
        .andExpect(content().string(containsString("Liga Inca")));
  }

  @Test
  void comunidadMuestraLaSemanaYElHistorial() throws Exception {
    when(rankingService.obtenerRankingDelSalon(1L))
        .thenReturn(
            new RankingSalon(
                1,
                "A",
                List.of(new RankingSalon.Posicion(1, "Ana", "✨", 130, true)),
                1,
                130,
                60,
                250,
                java.time.LocalDate.of(2026, 11, 9),
                java.time.LocalDate.of(2026, 11, 15)));

    mvc.perform(get("/estudiante/comunidad").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Semana del 09/11 al 15/11")))
        .andExpect(content().string(containsString("¡Estás en la liga más alta!")))
        .andExpect(content().string(containsString("60 XP la semana pasada · 250 XP en total")));
  }

  @Test
  void hu21ComunidadUsaLaLigaGuardadaYMuestraLaNotificacion() throws Exception {
    when(ligaService.actualizarSalonYNotificar(1L))
        .thenReturn(List.of("🎉 ¡Ascendiste a Liga Auqui 🥇! Alcanzaste 80 XP esta semana."));
    when(rankingService.obtenerRankingDelSalon(1L))
        .thenReturn(
            new RankingSalon(
                1,
                "A",
                List.of(
                    new RankingSalon.Posicion(
                        1, "Ana", "✨", 20, true, kanichi.model.Liga.AUQUI)),
                1,
                20,
                0,
                200,
                null,
                null,
                kanichi.model.Liga.AUQUI));

    mvc.perform(get("/estudiante/comunidad").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Ascendiste a Liga Auqui")))
        .andExpect(content().string(containsString("<strong>Liga Auqui</strong>")))
        .andExpect(content().string(containsString("Te faltan 100 XP para subir a Liga Inca")))
        .andExpect(
            content()
                .string(
                    containsString("Gana 60 XP más esta semana para mantenerte en Liga Auqui")))
        .andExpect(content().string(containsString("href=\"/estudiante/desafios\"")));
  }

  @Test
  void hu21InicioMuestraLaNotificacionDeLiga() throws Exception {
    when(ligaService.actualizarYNotificar(1L))
        .thenReturn(List.of("🎉 ¡Ascendiste a Liga Panaca 🥈! Alcanzaste 40 XP esta semana."));

    mvc.perform(get("/inicio").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Ascendiste a Liga Panaca")));
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