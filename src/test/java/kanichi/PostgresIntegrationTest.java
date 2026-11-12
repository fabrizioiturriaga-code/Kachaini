package kanichi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.*;
import kanichi.dto.RegistroForm;
import kanichi.model.Leccion;
import kanichi.model.Rol;
import kanichi.repository.UsuarioRepository;
import kanichi.service.AuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresIntegrationTest {
  private static final String RUTA_LECCION = "/estudiante/lecciones/primeros-pobladores";
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    String urlLocal = System.getenv("TEST_DB_URL");
    if (urlLocal != null && !urlLocal.isBlank()) {
      r.add("spring.datasource.url", () -> urlLocal);
      r.add("spring.datasource.username", () -> System.getenv("TEST_DB_USERNAME"));
      r.add(
          "spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
      return;
    }
    pg.start();
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
  }

  @AfterAll
  static void detenerContenedor() {
    if (pg.isRunning()) {
      pg.stop();
    }
  }

  @Autowired MockMvc mvc;
  @Autowired AuthService auth;
  @Autowired UsuarioRepository usuarios;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute(
        "TRUNCATE docentes_autorizaciones, docentes, estudiantes, usuarios RESTART IDENTITY"
            + " CASCADE");
  }

  RegistroForm form(String correo) {
    var formulario = new RegistroForm();
    formulario.setNombre("Ana");
    formulario.setCorreo(correo);
    formulario.setContrasena("Historia123");
    formulario.setConfirmacion("Historia123");
    formulario.setRol(Rol.ESTUDIANTE);
    formulario.setGrado(1);
    formulario.setSeccion("A");
    return formulario;
  }

  @Test
  void joinedYDocente() {
    auth.registrar(form("a@example.org"));
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from estudiantes e join usuarios usuario on usuario.id=e.id",
            Integer.class));
    var formulario = form("d@example.org");
    formulario.setRol(Rol.DOCENTE);
    formulario.setGrado(null);
    formulario.setSeccion(null);
    formulario.setArea("Historia");
    auth.registrar(formulario);
    assertFalse(usuarios.findByCorreo("d@example.org").orElseThrow().isAutorizado());
    jdbc.update("update docentes set autorizado=true");
    assertTrue(usuarios.findByCorreo("d@example.org").orElseThrow().isAutorizado());
  }

  @Test
  void falloRevierteTodaLaTransaccion() {
    jdbc.execute("ALTER TABLE estudiantes ADD CONSTRAINT ck_prueba CHECK (grado <> 5)");
    try {
      var formulario = form("x@example.org");
      formulario.setGrado(5);
      assertThrows(DataIntegrityViolationException.class, () -> auth.registrar(formulario));
      assertEquals(0, usuarios.count());
    } finally {
      jdbc.execute("ALTER TABLE estudiantes DROP CONSTRAINT ck_prueba");
    }
  }

  @Test
  void registrosConcurrentesUnicos() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var inicio = new CountDownLatch(1);
    Callable<Boolean> tarea =
        () -> {
          inicio.await();
          try {
            auth.registrar(form("unico@example.org"));
            return true;
          } catch (RuntimeException e) {
            if (e instanceof DataIntegrityViolationException
                || e instanceof kanichi.exception.RegistroInvalidoException) return false;
            throw e;
          }
        };
    try {
      var a = executor.submit(tarea);
      var b = executor.submit(tarea);
      inicio.countDown();
      assertNotEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
      assertEquals(1, usuarios.count());
      assertEquals(1, jdbc.queryForObject("select count(*) from estudiantes", Integer.class));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void registroLoginYLogoutConPostgres() throws Exception {
    mvc.perform(
            post("/registro")
                .with(csrf())
                .param("nombre", "Ana")
                .param("correo", " ANA@EXAMPLE.ORG ")
                .param("contrasena", "Historia123")
                .param("confirmacion", "Historia123")
                .param("rol", "ESTUDIANTE")
                .param("grado", "1")
                .param("seccion", "A"))
        .andExpect(redirectedUrl("/login"));
    assertTrue(usuarios.existsByCorreo("ana@example.org"));

    var resultadoLogin =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("correo", "ana@example.org")
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/onboarding"))
            .andReturn();
    var sesion = (MockHttpSession) resultadoLogin.getRequest().getSession(false);
    assertNotNull(sesion);
    mvc.perform(get("/onboarding").session(sesion)).andExpect(status().isOk());
    mvc.perform(post("/logout").session(sesion).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"));
    assertTrue(sesion.isInvalid());
  }

  @Test
  void completarModuloInicialPersisteRecompensasYDesbloqueaInicio() throws Exception {
    auth.registrar(form("ana@example.org"));
    var sesion =
        (MockHttpSession)
            mvc.perform(
                    post("/login")
                        .with(csrf())
                        .param("correo", "ana@example.org")
                        .param("contrasena", "Historia123"))
                .andExpect(redirectedUrl("/onboarding"))
                .andReturn()
                .getRequest()
                .getSession(false);

    mvc.perform(get("/inicio").session(sesion)).andExpect(redirectedUrl("/onboarding"));
    for (String mision : new String[] {"bienvenida", "que-es-la-historia", "linea-de-tiempo"}) {
      mvc.perform(post("/onboarding/misiones/" + mision).session(sesion).with(csrf()))
          .andExpect(redirectedUrl("/onboarding"));
    }
    mvc.perform(
            post("/onboarding/misiones/reto-de-bienvenida")
                .param("respuesta", "1")
                .session(sesion)
                .with(csrf()))
        .andExpect(redirectedUrl("/inicio"));
    mvc.perform(get("/inicio").session(sesion)).andExpect(status().isOk());

    assertEquals(
        kanichi.model.MisionInicial.xpTotal(),
        jdbc.queryForObject("SELECT xp FROM estudiantes", Integer.class));
    assertEquals(
        Boolean.TRUE,
        jdbc.queryForObject("SELECT onboarding_completado FROM estudiantes", Boolean.class));
    assertEquals(
        4, jdbc.queryForObject("SELECT count(*) FROM misiones_completadas", Integer.class));
    assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM logros_estudiante", Integer.class));
  }

  @Test
  void revocarDocenteInvalidaSesionYRegistraAuditoria() throws Exception {
    RegistroForm docente = form("docente@example.org");
    docente.setRol(Rol.DOCENTE);
    docente.setGrado(null);
    docente.setSeccion(null);
    docente.setArea("Historia");
    auth.registrar(docente);
    jdbc.update("UPDATE docentes SET autorizado=true");
    var resultado =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("correo", "docente@example.org")
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/docente/panel"))
            .andReturn();
    var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
    mvc.perform(get("/docente/panel").session(sesion)).andExpect(status().isOk());
    jdbc.update("UPDATE docentes SET autorizado=false");
    mvc.perform(get("/docente/panel").session(sesion)).andExpect(redirectedUrl("/login?revocada"));
    assertTrue(sesion.isInvalid());
    assertEquals(
        2, jdbc.queryForObject("SELECT count(*) FROM docentes_autorizaciones", Integer.class));
  }

  @Test
  void intentosFallidosProducenEsperaTemporal() throws Exception {
    for (int intento = 0; intento < 5; intento++) {
      mvc.perform(
              post("/login")
                  .servletPath("/login")
                  .with(csrf())
                  .with(
                      request -> {
                        request.setRemoteAddr("192.0.2.10");
                        return request;
                      })
                  .param("correo", "nadie@example.org")
                  .param("contrasena", "Incorrecta123"))
          .andExpect(redirectedUrl("/login?error"));
    }
    mvc.perform(
            post("/login")
                .servletPath("/login")
                .with(csrf())
                .with(
                    request -> {
                      request.setRemoteAddr("192.0.2.10");
                      return request;
                    })
                .header("X-Forwarded-For", "192.0.2.20")
                .param("correo", "nadie@example.org")
                .param("contrasena", "Incorrecta123"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void recrearCuentaNoReutilizaSesionAnterior() throws Exception {
    auth.registrar(form("ana@example.org"));
    var resultado =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("correo", "ana@example.org")
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/onboarding"))
            .andReturn();
    var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
    jdbc.update("DELETE FROM estudiantes");
    jdbc.update("DELETE FROM usuarios");
    auth.registrar(form("ana@example.org"));
    mvc.perform(get("/onboarding").session(sesion)).andExpect(redirectedUrl("/login?revocada"));
    assertTrue(sesion.isInvalid());
  }

  @Test
  void feedbackEnLaLeccionYReenvioSinDobleDescuentoNiXp() throws Exception {
    var sesion = ingresarConOnboarding("ana@example.org");
    String intento = UUID.randomUUID().toString();
    var pregunta = Leccion.U1_PRIMEROS_POBLADORES.getPregunta();

    mvc.perform(
            post(RUTA_LECCION)
                .session(sesion)
                .with(csrf())
                .param("respuesta", "0")
                .param("intento", intento))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(pregunta.pista())))
        .andExpect(content().string(not(containsString(pregunta.explicacion()))));
    mvc.perform(
            post(RUTA_LECCION)
                .session(sesion)
                .with(csrf())
                .param("respuesta", "0")
                .param("intento", intento))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Esta respuesta ya fue evaluada.")));
    assertEquals(4, jdbc.queryForObject("SELECT energia FROM estudiantes", Integer.class));

    String acierto = UUID.randomUUID().toString();
    mvc.perform(
            post(RUTA_LECCION)
                .session(sesion)
                .with(csrf())
                .param("respuesta", "1")
                .param("intento", acierto))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(pregunta.explicacion())))
        .andExpect(content().string(containsString("Continuar a la unidad")));
    mvc.perform(
            post(RUTA_LECCION)
                .session(sesion)
                .with(csrf())
                .param("respuesta", "1")
                .param("intento", acierto))
        .andExpect(redirectedUrl("/estudiante/unidades/1"));
    assertEquals(20, jdbc.queryForObject("SELECT xp FROM estudiantes", Integer.class));
    assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM intentos_leccion", Integer.class));
  }

  @Test
  void guardaTextoUnaVezYLoMuestraSoloAQuienLoEnvio() throws Exception {
    var sesion = ingresarConOnboarding("ana@example.org");
    String rutaEntrega = RUTA_LECCION + "/desarrollo";
    String respuesta = "Buscaban alimentos.\nTambién cazaban y recolectaban.";

    mvc.perform(post(rutaEntrega).session(sesion).with(csrf()).param("texto", " ".repeat(5)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Escribe una respuesta")));
    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM entregas_desarrollo", Integer.class));
    var envio =
        mvc.perform(post(rutaEntrega).session(sesion).with(csrf()).param("texto", respuesta))
            .andExpect(redirectedUrl(RUTA_LECCION))
            .andExpect(flash().attribute("mensajeDesarrollo", "Respuesta enviada correctamente."))
            .andReturn();
    mvc.perform(get(RUTA_LECCION).session(sesion).flashAttrs(envio.getFlashMap()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Respuesta enviada correctamente.")))
        .andExpect(content().string(containsString(respuesta)));
    mvc.perform(post(rutaEntrega).session(sesion).with(csrf()).param("texto", "Otro texto"))
        .andExpect(redirectedUrl(RUTA_LECCION))
        .andExpect(
            flash()
                .attribute("mensajeDesarrollo", "Ya enviaste una respuesta para esta actividad."));
    assertEquals(
        respuesta, jdbc.queryForObject("SELECT texto FROM entregas_desarrollo", String.class));
    assertEquals(0, jdbc.queryForObject("SELECT xp FROM estudiantes", Integer.class));
    assertEquals(5, jdbc.queryForObject("SELECT energia FROM estudiantes", Integer.class));
    assertEquals(
        0, jdbc.queryForObject("SELECT count(*) FROM lecciones_completadas", Integer.class));

    var otraSesion = ingresarConOnboarding("beto@example.org");
    mvc.perform(get(RUTA_LECCION).session(otraSesion))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString(respuesta))))
        .andExpect(content().string(containsString("Enviar respuesta")));
  }

  private MockHttpSession ingresarConOnboarding(String correo) throws Exception {
    auth.registrar(form(correo));
    jdbc.update(
        "UPDATE estudiantes SET onboarding_completado=true "
            + "WHERE id=(SELECT id FROM usuarios WHERE correo=?)",
        correo);
    return (MockHttpSession)
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("correo", correo)
                    .param("contrasena", "Historia123"))
            .andExpect(redirectedUrl("/inicio"))
            .andReturn()
            .getRequest()
            .getSession(false);
  }
}
