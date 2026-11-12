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
import kanichi.controller.ArrastreController;
import kanichi.dto.ActividadPractica;
import kanichi.dto.UsuarioSesion;
import kanichi.model.ActividadArrastre;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Rol;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.ArrastreService;
import kanichi.service.MapaMisionesService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArrastreController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class, LimiteIntentosLogin.class})
class ArrastreControllerTest {
  private static final ActividadArrastre ACTIVIDAD = ActividadArrastre.LEGADO_PREINCA;
  private static final String RUTA = "/estudiante/arrastre/" + ACTIVIDAD.getCodigo();

  private final ArrastreService corrector =
      new ArrastreService(mock(EstudianteRepository.class), new MapaMisionesService());

  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean ArrastreService arrastreService;

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

    when(arrastreService.estaDisponible(1L, ACTIVIDAD)).thenReturn(true);
    when(arrastreService.corregir(any(), any()))
        .thenAnswer(i -> corrector.corregir(i.getArgument(0), i.getArgument(1)));
  }

  @Test
  void laListaMuestraLasActividades() throws Exception {
    when(arrastreService.listar(1L))
        .thenReturn(
            List.of(
                new ActividadPractica(
                    "legado-preinca", "Culturas preíncas y su legado", 2, "Culturas", true)));

    mvc.perform(get("/estudiante/arrastre").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/practica"))
        .andExpect(content().string(containsString("Arrastrar y soltar")))
        .andExpect(content().string(containsString("href=\"/estudiante/arrastre/legado-preinca\"")));
  }

  @Test
  void laActividadMuestraLosDestinosYLosElementos() throws Exception {
    mvc.perform(get(RUTA).with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/arrastre"))
        .andExpect(content().string(containsString("Arrastra cada cultura hasta su aporte")))
        .andExpect(content().string(containsString("Mantos funerarios bordados")))
        .andExpect(content().string(containsString("name=\"respuestas[3]\"")))
        .andExpect(content().string(containsString("data-zona")))
        .andExpect(content().string(containsString("Chimú")))
        .andExpect(content().string(containsString("Verificar")));
  }

  @Test
  void alVerificarIndicaQueEmparejamientosSonCorrectosYCualesNo() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "1")
                .param("respuestas[1]", "0")
                .param("respuestas[2]", "2")
                .param("respuestas[3]", "3")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Emparejamientos correctos: 2 de 4.")))
        .andExpect(content().string(containsString("✅ Correcto")))
        .andExpect(content().string(containsString("❌ Incorrecto")))
        .andExpect(content().string(containsString("arrastre__destino--incorrecto")))
        .andExpect(content().string(containsString("selected")))
        .andExpect(content().string(containsString("Verificar")));
  }

  @Test
  void todoCorrectoFelicitaYPermiteElegirOtraActividad() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "0")
                .param("respuestas[1]", "1")
                .param("respuestas[2]", "2")
                .param("respuestas[3]", "3")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(content().string(containsString("Emparejamientos correctos: 4 de 4.")))
        .andExpect(content().string(containsString("¡Excelente!")))
        .andExpect(content().string(containsString("Elegir otra actividad")))
        .andExpect(content().string(not(containsString("❌ Incorrecto"))));
  }

  @Test
  void siFaltanElementosPorUbicarLoAvisa() throws Exception {
    mvc.perform(
            post(RUTA)
                .param("respuestas[0]", "0")
                .param("respuestas[1]", "1")
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ubica todos los elementos antes de verificar.")));
    verify(arrastreService, never()).corregir(any(), any());
  }

  @Test
  void unaActividadBloqueadaVuelveALaLista() throws Exception {
    when(arrastreService.estaDisponible(1L, ActividadArrastre.ORGANIZACION_INCA)).thenReturn(false);

    mvc.perform(get("/estudiante/arrastre/organizacion-inca").with(user(estudiante())))
        .andExpect(redirectedUrl("/estudiante/arrastre"))
        .andExpect(flash().attribute("error", "Completa primero la unidad anterior."));
  }

  @Test
  void unaActividadInexistenteDevuelve404() throws Exception {
    mvc.perform(get("/estudiante/arrastre/no-existe").with(user(estudiante())))
        .andExpect(status().isNotFound());
  }

  @Test
  void anonimoVaAlLoginYDocenteNoAccede() throws Exception {
    mvc.perform(get(RUTA))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    mvc.perform(get(RUTA).with(user(docente()))).andExpect(status().isForbidden());
  }

  @Test
  void sinCompletarElModuloInicialVaAlOnboarding() throws Exception {
    var pendiente =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);

    mvc.perform(get("/estudiante/arrastre").with(user(pendiente)))
        .andExpect(redirectedUrl("/onboarding"));
  }

  private UsuarioSesion estudiante() {
    return new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, true);
  }

  private UsuarioSesion docente() {
    return new UsuarioSesion(
        2L, "Docente", "docente@example.org", "hash", Rol.DOCENTE, true, false);
  }
}
