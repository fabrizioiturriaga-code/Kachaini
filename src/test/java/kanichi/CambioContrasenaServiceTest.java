package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import kanichi.dto.CambioContrasenaForm;
import kanichi.exception.CambioContrasenaInvalidoException;
import kanichi.model.Estudiante;
import kanichi.repository.UsuarioRepository;
import kanichi.service.CambioContrasenaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CambioContrasenaServiceTest {
  private final UsuarioRepository repository = mock(UsuarioRepository.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final CambioContrasenaService service =
      new CambioContrasenaService(repository, encoder);
  private final Estudiante estudiante =
      new Estudiante("Ana", "ana@example.org", encoder.encode("Historia123"), 1, "A");

  @BeforeEach
  void setup() {
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
  }

  @Test
  void actualizaHashAlValidarLasTresContrasenas() {
    CambioContrasenaForm formulario = formulario("Historia123", "NuevaClave456", "NuevaClave456");

    service.cambiar(1L, formulario);

    assertTrue(encoder.matches("NuevaClave456", estudiante.getContrasenaHash()));
    assertFalse(encoder.matches("Historia123", estudiante.getContrasenaHash()));
    verify(repository).save(estudiante);
  }

  @Test
  void rechazaContrasenaActualIncorrecta() {
    CambioContrasenaForm formulario = formulario("Incorrecta123", "NuevaClave456", "NuevaClave456");

    var error =
        assertThrows(
            CambioContrasenaInvalidoException.class, () -> service.cambiar(1L, formulario));

    assertEquals("contrasenaActual", error.getCampo());
    verify(repository, never()).save(any());
  }

  @Test
  void rechazaNuevaContrasenaDebil() {
    CambioContrasenaForm formulario = formulario("Historia123", "debil", "debil");

    var error =
        assertThrows(
            CambioContrasenaInvalidoException.class, () -> service.cambiar(1L, formulario));

    assertEquals("contrasenaNueva", error.getCampo());
    verify(repository, never()).save(any());
  }

  @Test
  void rechazaConfirmacionDistinta() {
    CambioContrasenaForm formulario = formulario("Historia123", "NuevaClave456", "OtraClave789");

    var error =
        assertThrows(
            CambioContrasenaInvalidoException.class, () -> service.cambiar(1L, formulario));

    assertEquals("confirmacion", error.getCampo());
    verify(repository, never()).save(any());
  }

  private CambioContrasenaForm formulario(String actual, String nueva, String confirmacion) {
    CambioContrasenaForm formulario = new CambioContrasenaForm();
    formulario.setContrasenaActual(actual);
    formulario.setContrasenaNueva(nueva);
    formulario.setConfirmacion(confirmacion);
    return formulario;
  }
}
