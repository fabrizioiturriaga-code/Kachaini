package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kanichi.config.SecurityConfig;
import kanichi.controller.LeccionController;
import kanichi.dto.EstadoEnergia;
import kanichi.dto.ResultadoLeccion;
import kanichi.dto.ResultadoLeccion.Resultado;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadMapa.Estado;
import kanichi.dto.UsuarioSesion;
import kanichi.dto.VistaLeccion;
import kanichi.dto.VistaUnidad;
import kanichi.dto.VistaUnidad.EstadoLeccion;
import kanichi.exception.MisionNoPermitidaException;
import kanichi.model.Docente;
import kanichi.model.Estudiante;
import kanichi.model.Leccion;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.security.LimiteIntentosLogin;
import kanichi.service.LeccionService;
import kanichi.service.MapaMisionesService;
import kanichi.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** HU-18: unidades y lecciones. HU-12: barra de energía y bloqueo al agotarla. */
@WebMvcTest(LeccionController.class)
@Import({
  SecurityConfig.class,
  UsuarioDetailsService.class,
  LimiteIntentosLogin.class,
  MapaMisionesService.class
})
class LeccionControllerTest {
  private static final Leccion LECCION = Leccion.U1_PRIMEROS_POBLADORES;
  private static final String RUTA = "/estudiante/lecciones/" + LECCION.getCodigo();

  private static final UUID INTENTO = UUID.fromString("a49faf67-726b-48f9-8d33-ae3b0c02fa5d");

  @Autowired MockMvc mvc;

  @MockBean UsuarioRepository usuarios;
  @MockBean LimiteIntentosLogin limiteIntentosLogin;
  @MockBean LeccionService leccionService;

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
  }

  @Test
  void laUnidadMuestraSusLeccionesYLaEnergia() throws Exception {
    List<EstadoLeccion> lecciones =
        List.of(
            new EstadoLeccion(Leccion.U1_PRIMEROS_POBLADORES, true, true),
            new EstadoLeccion(Leccion.U1_CAZADORES_DEL_PERU, false, true),
            new EstadoLeccion(Leccion.U1_AGRICULTURA, false, false));
    when(leccionService.obtenerUnidad(1L, 1))
        .thenReturn(
            new VistaUnidad(
                new UnidadMapa(1, "Poblamiento", Estado.ACTIVA, 1, 3),
                lecciones,
                new EstadoEnergia(5, 5, 0)));

    mvc.perform(get("/estudiante/unidades/1").with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/unidad"))
        .andExpect(content().string(containsString("Unidad 1: Poblamiento")))
        .andExpect(
            content().string(containsString("href=\"/estudiante/lecciones/cazadores-del-peru\"")))
        .andExpect(content().string(containsString("✅ Completada")))
        .andExpect(content().string(containsString("🔒 Bloqueada")))
        .andExpect(content().string(containsString("5/5")));
  }

  @Test
  void unaUnidadBloqueadaRedirigeAlMapa() throws Exception {
    when(leccionService.obtenerUnidad(1L, 2))
        .thenReturn(
            new VistaUnidad(
                new UnidadMapa(2, "Culturas", Estado.BLOQUEADA, 0, 3),
                List.of(),
                new EstadoEnergia(5, 5, 0)));

    mvc.perform(get("/estudiante/unidades/2").with(user(estudiante())))
        .andExpect(redirectedUrl("/inicio"))
        .andExpect(flash().attribute("error", "Completa primero la unidad anterior."));
  }

  @Test
  void unaUnidadInexistenteDevuelve404() throws Exception {
    mvc.perform(get("/estudiante/unidades/99").with(user(estudiante())))
        .andExpect(status().isNotFound());
  }

  @Test
  void laLeccionMuestraLaPreguntaYLaBarraDeEnergia() throws Exception {
    when(leccionService.obtenerLeccion(1L, LECCION))
        .thenReturn(new VistaLeccion(LECCION, false, true, new EstadoEnergia(3, 5, 120)));

    mvc.perform(get(RUTA).with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/leccion"))
        .andExpect(content().string(containsString("¿Cómo vivían los primeros pobladores")))
        .andExpect(content().string(containsString("aria-valuenow=\"3\"")))
        .andExpect(content().string(containsString("3/5")))
        .andExpect(content().string(containsString("Próxima recarga en 2 min")))
        .andExpect(content().string(containsString("Responder")));
  }

  @Test
  void conLaEnergiaAgotadaNoSeMuestraElFormulario() throws Exception {
    when(leccionService.obtenerLeccion(1L, LECCION))
        .thenReturn(new VistaLeccion(LECCION, false, true, new EstadoEnergia(0, 5, 540)));

    mvc.perform(get(RUTA).with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Sin energía. Vuelve en 9 min")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(containsString("data-opcion-multiple"))));
  }

  @Test
  void unaLeccionBloqueadaRedirigeAlMapa() throws Exception {
    when(leccionService.obtenerLeccion(1L, Leccion.U1_AGRICULTURA))
        .thenReturn(
            new VistaLeccion(Leccion.U1_AGRICULTURA, false, false, new EstadoEnergia(5, 5, 0)));

    mvc.perform(get("/estudiante/lecciones/agricultura-y-aldeas").with(user(estudiante())))
        .andExpect(redirectedUrl("/inicio"))
        .andExpect(flash().attribute("error", "Completa primero la lección anterior."));
  }

  @Test
  void unaLeccionInexistenteDevuelve404() throws Exception {
    mvc.perform(get("/estudiante/lecciones/no-existe").with(user(estudiante())))
        .andExpect(status().isNotFound());
  }

  @Test
  void unErrorMuestraLaPistaYLaEnergiaRestante() throws Exception {
    when(leccionService.responder(1L, LECCION, 0, INTENTO))
        .thenReturn(
            new ResultadoLeccion(
                Resultado.INCORRECTA, 0, new EstadoEnergia(4, 5, 600), false, null));

    mvc.perform(
            post(RUTA)
                .param("respuesta", "0")
                .param("intento", INTENTO.toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(view().name("estudiante/leccion"))
        .andExpect(content().string(containsString("Respuesta incorrecta. Perdiste 1 de energía")))
        .andExpect(content().string(containsString("te quedan 4 de 5")))
        .andExpect(content().string(containsString(LECCION.getPregunta().pista())))
        .andExpect(content().string(containsString("4/5")));
  }

  @Test
  void elUltimoErrorAvisaQueSeAgotoLaEnergia() throws Exception {
    when(leccionService.responder(1L, LECCION, 0, INTENTO))
        .thenReturn(
            new ResultadoLeccion(
                Resultado.INCORRECTA, 0, new EstadoEnergia(0, 5, 600), false, null));

    mvc.perform(
            post(RUTA)
                .param("respuesta", "0")
                .param("intento", INTENTO.toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Te quedaste sin energía. Espera 10 min")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(containsString("data-opcion-multiple"))));
  }

  @Test
  void respuestaCorrectaExplicaAntesDeContinuarALaUnidad() throws Exception {
    when(leccionService.responder(1L, LECCION, 1, INTENTO))
        .thenReturn(
            new ResultadoLeccion(Resultado.CORRECTA, 20, new EstadoEnergia(5, 5, 0), false, null));

    mvc.perform(
            post(RUTA)
                .param("respuesta", "1")
                .param("intento", INTENTO.toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Correcto! Ganaste 20 XP.")))
        .andExpect(content().string(containsString(LECCION.getPregunta().explicacion())))
        .andExpect(content().string(containsString("Continuar a la unidad")));
  }

  @Test
  void completarUnaUnidadAvisaDelNuevoNivelYPermiteVolverAlMapa() throws Exception {
    when(leccionService.responder(1L, Leccion.U1_AGRICULTURA, 0, INTENTO))
        .thenReturn(
            new ResultadoLeccion(Resultado.CORRECTA, 20, new EstadoEnergia(5, 5, 0), true, 2));

    mvc.perform(
            post("/estudiante/lecciones/agricultura-y-aldeas")
                .param("respuesta", "0")
                .param("intento", INTENTO.toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("¡Completaste la unidad 1!")))
        .andExpect(
            content()
                .string(containsString("¡Nuevo nivel disponible! Unidad 2: Culturas preíncas.")))
        .andExpect(content().string(containsString("Continuar al mapa")));
  }

  @Test
  void unaLeccionFueraDeOrdenVuelveAlMapaConUnMensaje() throws Exception {
    when(leccionService.responder(any(), any(), any(), any()))
        .thenThrow(new MisionNoPermitidaException("Completa primero la lección anterior."));

    mvc.perform(
            post(RUTA)
                .param("respuesta", "0")
                .param("intento", INTENTO.toString())
                .with(csrf())
                .with(user(estudiante())))
        .andExpect(redirectedUrl("/inicio"))
        .andExpect(flash().attribute("error", "Completa primero la lección anterior."));
  }

  @Test
  void anonimoEsRedirigidoAlLoginYDocenteNoAccede() throws Exception {
    mvc.perform(get(RUTA))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    mvc.perform(get(RUTA).with(user(docente()))).andExpect(status().isForbidden());
    mvc.perform(get("/estudiante/unidades/1").with(user(docente())))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinCompletarElModuloInicialVaAlOnboarding() throws Exception {
    var pendiente =
        new UsuarioSesion(1L, "Ana", "ana@example.org", "hash", Rol.ESTUDIANTE, true, false);

    mvc.perform(get(RUTA).with(user(pendiente))).andExpect(redirectedUrl("/onboarding"));
    mvc.perform(get("/estudiante/unidades/1").with(user(pendiente)))
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
