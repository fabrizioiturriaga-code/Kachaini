package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import kanichi.model.Estudiante;
import kanichi.model.MisionInicial;
import kanichi.repository.EstudianteRepository;
import kanichi.service.RankingService;
import org.junit.jupiter.api.Test;

class RankingServiceTest {
  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final RankingService service = new RankingService(repository);

  @Test
  void ordenaPorXpYLosEmpatesCompartenPuesto() {
    Estudiante ana = estudiante(1L, "Ana", MisionInicial.BIENVENIDA, MisionInicial.QUE_ES_LA_HISTORIA);
    Estudiante luis = estudiante(2L, "Luis", MisionInicial.BIENVENIDA);
    Estudiante rosa = estudiante(3L, "Rosa", MisionInicial.BIENVENIDA);
    when(repository.findById(2L)).thenReturn(Optional.of(luis));
    when(repository.findTop10ByGradoAndSeccionOrderByXpDescNombreAsc(1, "A"))
        .thenReturn(List.of(ana, luis, rosa));
    when(repository.countByGradoAndSeccionAndXpGreaterThan(1, "A", luis.getXp())).thenReturn(1L);

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals(List.of(1, 2, 2), ranking.posiciones().stream().map(p -> p.puesto()).toList());
    assertTrue(ranking.posiciones().get(1).esActual());
    assertFalse(ranking.posiciones().get(0).esActual());
    assertEquals(2, ranking.miPosicion());
    assertEquals(luis.getXp(), ranking.miXp());
  }

  private static Estudiante estudiante(Long id, String nombre, MisionInicial... misiones) {
    Estudiante estudiante = spy(new Estudiante(nombre, nombre + "@example.org", "hash", 1, "A"));
    doReturn(id).when(estudiante).getId();
    for (MisionInicial mision : misiones) {
      estudiante.registrarMision(mision);
    }
    return estudiante;
  }
}
