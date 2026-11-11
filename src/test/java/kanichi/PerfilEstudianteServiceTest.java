package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import kanichi.dto.PerfilEstudianteForm;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.repository.UsuarioRepository;
import kanichi.service.PerfilEstudianteService;
import org.junit.jupiter.api.Test;

class PerfilEstudianteServiceTest {
  private final UsuarioRepository repository = mock(UsuarioRepository.class);
  private final PerfilEstudianteService service = new PerfilEstudianteService(repository);

  @Test
  void actualizaNombreVisibleYAvatar() {
    Estudiante estudiante = new Estudiante("Ana", "ana@example.org", "hash", 1, "A");
    when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
    PerfilEstudianteForm formulario = new PerfilEstudianteForm();
    formulario.setNombre("  Ana María  ");
    formulario.setAvatar("COLIBRI");

    service.actualizar(1L, formulario);

    assertEquals("Ana María", estudiante.getNombre());
    assertEquals(AvatarEstudiante.COLIBRI, estudiante.getAvatar());
    verify(repository).save(estudiante);
  }
}
