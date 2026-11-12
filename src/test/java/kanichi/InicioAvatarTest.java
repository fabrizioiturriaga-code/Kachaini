package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class InicioAvatarTest {
  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ModuloInicialService moduloInicialService;
  @MockBean RankingService rankingService;
  @MockBean LeccionService leccionService;

  @BeforeEach
  void setup() {
    when(leccionService.obtenerMapa(1L))
        .thenReturn(
            new MapaAprendizaje(
                new MapaMisionesService().obtenerMapa(1, true), new EstadoEnergia(4, 5, 300)));
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

  @Test
  void elSaludoDeInicioMuestraElAvatarDelEstudiante() throws Exception {
    UsuarioSesion sesion =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
    sesion.actualizarAvatar("🦊");

    mvc.perform(get("/inicio").with(user(sesion)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"saludo__avatar\"")))
        .andExpect(content().string(containsString("🦊")));
  }

  @Test
  void sinAvatarElSaludoSeMuestraIgual() throws Exception {
    UsuarioSesion sesion =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);

    mvc.perform(get("/inicio").with(user(sesion)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Hola,")))
        .andExpect(content().string(not(containsString("saludo__avatar"))));
  }
}
