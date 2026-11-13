package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kanichi.config.SecurityConfig;
import kanichi.controller.RetoController;
import kanichi.dto.AvisoReto;
import kanichi.dto.ResumenAvisos;
import kanichi.dto.RetoPublicado;
import kanichi.dto.UsuarioSesion;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.RetoDocente;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.RetoDocenteService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(RetoController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class RetoControllerTest {
  private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean RetoDocenteService retoDocenteService;

  private final RetoDocente reto =
      new RetoDocente(
          2L,
          2,
          "C",
          "Energía Inca",
          "Repasa el Tahuantinsuyo antes del viernes.",
          "Completar la unidad 3\nResponder sin errores",
          FECHA,
          30,
          Instant.parse("2026-11-12T18:00:00Z"));

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

    when(retoDocenteService.publicadosPor(2L))
        .thenReturn(List.of(new RetoPublicado(reto, 25, 10)));
    when(retoDocenteService.resumen(1L)).thenReturn(new ResumenAvisos(1, 0));
  }

  @Test
  void elDocenteVeElFormularioYSusRetosPublicados() throws Exception {
    mvc.perform(get("/docente/retos").with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/retos"))
        .andExpect(content().string(containsString("Publicar reto")))
        .andExpect(content().string(containsString("Energía Inca")))
        .andExpect(content().string(containsString("2.° C · Vence el 20/11/2026 · 30 XP")))
        .andExpect(content().string(containsString("Notificados: 25 · Lo abrieron: 10")));
  }

  @Test
  void publicarUnRetoNotificaYConfirma() throws Exception {
    when(retoDocenteService.publicar(
            eq(2L), eq(2), eq("C"), eq("Energía Inca"), any(), any(), eq(FECHA), eq(30)))
        .thenReturn(25);

    mvc.perform(
            post("/docente/retos")
                .param("grado", "2")
                .param("seccion", "C")
                .param("titulo", "Energía Inca")
                .param("descripcion", "Repasa el Tahuantinsuyo.")
                .param("objetivos", "Completar la unidad 3")
                .param("fechaLimite", "2026-11-20")
                .param("xp", "30")
                .with(csrf())
                .with(user(docenteSesion())))
        .andExpect(redirectedUrl("/docente/retos"))
        .andExpect(flash().attribute("mensaje", "Reto publicado. Se notificó a 25 estudiantes."));
  }

  @Test
  void unErrorVuelveAMostrarElFormularioConLosDatos() throws Exception {
    when(retoDocenteService.publicar(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("La fecha límite debe ser hoy o una fecha futura"));

    mvc.perform(
            post("/docente/retos")
                .param("grado", "2")
                .param("titulo", "Mi reto")
                .param("fechaLimite", "2020-01-01")
                .with(csrf())
                .with(user(docenteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("docente/retos"))
        .andExpect(content().string(containsString("La fecha límite debe ser hoy o una fecha futura")))
        .andExpect(content().string(containsString("value=\"Mi reto\"")));
  }

  @Test
  void elEstudianteVeSusAvisosConElContadorDeNuevos() throws Exception {
    when(retoDocenteService.avisos(1L))
        .thenReturn(List.of(new AvisoReto(reto, false, RetoDocente.Estado.POR_VENCER)));

    mvc.perform(get("/estudiante/avisos").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/avisos"))
        .andExpect(content().string(containsString("Energía Inca")))
        .andExpect(content().string(containsString("Nuevo")))
        .andExpect(content().string(containsString("Vence pronto")))
        .andExpect(content().string(containsString("Recompensa: 30 XP")))
        .andExpect(content().string(containsString("class=\"barra-inferior__contador\"")));
  }

  @Test
  void elDetalleMuestraDescripcionObjetivosFechaYRecompensa() throws Exception {
    when(retoDocenteService.abrir(1L, 7L))
        .thenReturn(new AvisoReto(reto, true, RetoDocente.Estado.VIGENTE));

    mvc.perform(get("/estudiante/avisos/7").with(user(estudianteSesion())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/aviso"))
        .andExpect(content().string(containsString("Repasa el Tahuantinsuyo antes del viernes.")))
        .andExpect(content().string(containsString("<li>Completar la unidad 3</li>")))
        .andExpect(content().string(containsString("<li>Responder sin errores</li>")))
        .andExpect(content().string(containsString("20/11/2026")))
        .andExpect(content().string(containsString("30 XP")));
  }

  @Test
  void unRetoAjenoDevuelve404() throws Exception {
    when(retoDocenteService.abrir(1L, 99L))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mvc.perform(get("/estudiante/avisos/99").with(user(estudianteSesion())))
        .andExpect(status().isNotFound());
  }

  @Test
  void sinAvisosNuevosNoSeMuestraElContador() throws Exception {
    when(retoDocenteService.resumen(1L)).thenReturn(ResumenAvisos.VACIO);
    when(retoDocenteService.avisos(1L)).thenReturn(List.of());

    mvc.perform(get("/estudiante/avisos").with(user(estudianteSesion())))
        .andExpect(content().string(containsString("No tienes avisos por ahora.")))
        .andExpect(content().string(not(containsString("class=\"barra-inferior__contador\""))));
  }

  @Test
  void cadaRolSoloEntraASuParte() throws Exception {
    mvc.perform(get("/docente/retos").with(user(estudianteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/avisos").with(user(docenteSesion())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/avisos"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  private UsuarioSesion estudianteSesion() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }

  private UsuarioSesion docenteSesion() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}