package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import kanichi.dto.UnidadMapa;
import kanichi.dto.UnidadMapa.Estado;
import kanichi.model.Leccion;
import kanichi.service.MapaMisionesService;
import org.junit.jupiter.api.Test;

/** HU-18: estados del mapa de aprendizaje y desbloqueo automático de niveles. */
class MapaMisionesServiceTest {
  private final MapaMisionesService service = new MapaMisionesService();

  @Test
  void sinCompletarElModuloInicialTodoEstaBloqueado() {
    List<UnidadMapa> mapa = service.obtenerMapa(false);

    assertEquals(6, mapa.size());
    assertTrue(mapa.stream().allMatch(UnidadMapa::isBloqueada));
  }

  @Test
  void alCompletarElModuloInicialSoloLaPrimeraUnidadEstaActiva() {
    List<UnidadMapa> mapa = service.obtenerMapa(true);

    assertEquals(Estado.ACTIVA, mapa.get(0).estado());
    assertTrue(mapa.subList(1, 6).stream().allMatch(UnidadMapa::isBloqueada));
    assertEquals(0, mapa.get(0).porcentaje());
    assertEquals("☆☆☆", mapa.get(0).estrellas());
  }

  @Test
  void elAvanceParcialSeMuestraEnElPorcentajeYLasEstrellas() {
    Set<Leccion> hechas = EnumSet.of(Leccion.U1_PRIMEROS_POBLADORES);

    UnidadMapa primera = service.obtenerMapa(true, hechas).get(0);

    assertEquals(Estado.ACTIVA, primera.estado());
    assertEquals(1, primera.leccionesCompletadas());
    assertEquals(33, primera.porcentaje());
    assertEquals("★☆☆", primera.estrellas());
  }

  @Test
  void completarTodasLasLeccionesDeUnaUnidadDesbloqueaLaSiguiente() {
    Set<Leccion> hechas = EnumSet.copyOf(Leccion.deUnidad(1));

    List<UnidadMapa> mapa = service.obtenerMapa(true, hechas);

    assertEquals(Estado.COMPLETADA, mapa.get(0).estado());
    assertEquals(100, mapa.get(0).porcentaje());
    assertEquals("★★★", mapa.get(0).estrellas());
    assertEquals(Estado.ACTIVA, mapa.get(1).estado());
    assertTrue(mapa.subList(2, 6).stream().allMatch(UnidadMapa::isBloqueada));
  }

  @Test
  void completarTodoElMapaDejaTodasLasUnidadesCompletadas() {
    List<UnidadMapa> mapa = service.obtenerMapa(true, EnumSet.allOf(Leccion.class));

    assertTrue(mapa.stream().allMatch(UnidadMapa::isCompletada));
  }

  @Test
  void laClaseCssDependeDelEstado() {
    assertEquals("unidad--bloqueada", new UnidadMapa(1, "U", Estado.BLOQUEADA).claseCss());
    assertEquals("unidad--activa", new UnidadMapa(1, "U", Estado.ACTIVA).claseCss());
    assertEquals("unidad--completada", new UnidadMapa(1, "U", Estado.COMPLETADA).claseCss());
  }
}
