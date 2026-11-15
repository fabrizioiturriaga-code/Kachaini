package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.Validation;
import kanichi.dto.RegistroForm;
import kanichi.exception.RegistroInvalidoException;
import kanichi.model.*;
import kanichi.repository.UsuarioRepository;
import kanichi.service.AuthService;
import kanichi.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
  UsuarioRepository repository;
  AuthService service;
  RegistroForm form;

  @BeforeEach
  void setup() {
    repository = mock(UsuarioRepository.class);
    service =
        new AuthService(
            repository,
            new BCryptPasswordEncoder(),
            Validation.buildDefaultValidatorFactory().getValidator(),
            new PoliticaCorreo(""));
    form = new RegistroForm();
    form.setNombre("Ana");
    form.setCorreo("  ANA@example.org ");
    form.setContrasena("Historia123");
    form.setConfirmacion("Historia123");
    form.setRol(Rol.ESTUDIANTE);
    form.setGrado(1);
    form.setSeccion("A");
  }

  @Test
  void estudianteConHash() {
    service.registrar(form);
    verify(repository)
        .saveAndFlush(
            argThat(
                usuario ->
                    usuario instanceof Estudiante e
                        && !e.isOnboardingCompletado()
                        && usuario.getCorreo().equals("ana@example.org")
                        && new BCryptPasswordEncoder()
                            .matches("Historia123", usuario.getContrasenaHash())));
  }

  @Test
  void docentePendiente() {
    form.setRol(Rol.DOCENTE);
    form.setGrado(null);
    form.setSeccion(null);
    form.setArea("Historia");
    service.registrar(form);
    verify(repository)
        .saveAndFlush(argThat(usuario -> usuario instanceof Docente && !usuario.isAutorizado()));
  }

  @Test
  void cnDocenteSinArea() {
    form.setRol(Rol.DOCENTE);
    form.setGrado(null);
    form.setSeccion(null);
    rechaza("area");
  }

  @Test
  void duplicado() {
    when(repository.existsByCorreo("ana@example.org")).thenReturn(true);
    assertThrows(RegistroInvalidoException.class, () -> service.registrar(form));
    verify(repository, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"corta1", "abcdefgh", "12345678", ""})
  void contrasenaInvalida(String value) {
    form.setContrasena(value);
    rechaza("contrasena");
  }

  @Test
  void confirmacion() {
    form.setConfirmacion("Otra1234");
    rechaza("confirmacion");
  }

  @Test
  void correo() {
    form.setCorreo("invalido");
    rechaza("correo");
  }

  @Test
  void obligatorio() {
    form.setNombre(" ");
    rechaza("nombre");
  }

  @Test
  void grado() {
    form.setGrado(6);
    rechaza("grado");
  }

  @Test
  void cbGradoNulo() {
    form.setGrado(null);
    rechaza("grado");
  }

  @Test
  void cbGradoMenorQueUno() {
    form.setGrado(0);
    rechaza("grado");
  }

  @Test
  void cnEstudianteValidoEnLimiteSuperior() {
    form.setGrado(5);
    form.setSeccion("C");
    service.registrar(form);
    verify(repository)
        .saveAndFlush(
            argThat(
                usuario -> usuario instanceof Estudiante estudiante
                    && estudiante.getGrado() == 5
                    && "C".equals(estudiante.getSeccion())));
  }

  @Test
  void seccion() {
    form.setSeccion("G");
    rechaza("seccion");
  }

  @Test
  void rolObligatorio() {
    form.setRol(null);
    rechaza("rol");
  }

  @Test
  void datosAjenoAlRol() {
    form.setArea("Historia");
    rechaza("area");
  }

  @Test
  void dominio() {
    assertFalse(new PoliticaCorreo("colegio.edu.pe").permite("ana@example.org"));
    assertEquals("ana@example.org", PoliticaCorreo.normalizar(" ANA@EXAMPLE.ORG "));
  }

  private void rechaza(String campo) {
    var e = assertThrows(RegistroInvalidoException.class, () -> service.registrar(form));
    assertTrue(e.getErrores().containsKey(campo));
    verify(repository, never()).saveAndFlush(any());
  }
}
