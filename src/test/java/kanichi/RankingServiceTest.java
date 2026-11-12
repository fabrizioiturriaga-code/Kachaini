package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanichi.model.Estudiante;
import kanichi.model.Liga;
import kanichi.model.MisionInicial;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.EstudianteRepository.XpDelPeriodo;
import kanichi.service.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-14: ranking semanal por XP y ligas. */
class RankingServiceTest {
  /** Jueves 12/11/2026 a las 13:00 en Lima. */
  private static final Instant JUEVES = Instant.parse("2026-11-12T18:00:00Z");

  /** Lunes 09/11/2026 a las 00:00 en Lima (UTC-5). */
  private static final Instant LUNES = Instant.parse("2026-11-09T05:00:00Z");

  private static final Instant LUNES_SIGUIENTE = Instant.parse("2026-11-16T05:00:00Z");
  private static final Instant LUNES_ANTERIOR = Instant.parse("2026-11-02T05:00:00Z");

  private final EstudianteRepository repository = mock(EstudianteRepository.class);
  private final RankingService service =
      new RankingService(repository, Clock.fixed(JUEVES, ZoneOffset.UTC));
  private Estudiante luis;

  @BeforeEach
  void setup() {
    luis = estudiante(2L, "Luis", MisionInicial.BIENVENIDA, MisionInicial.QUE_ES_LA_HISTORIA);
    when(repository.findById(2L)).thenReturn(Optional.of(luis));
  }

  @Test
  void ordenaPorXpDeLaSemanaYLosEmpatesCompartenPuesto() {
    when(repository.xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE))
        .thenReturn(
            List.of(
                fila(1L, "Ana", 60),
                fila(2L, "Luis", 40),
                fila(3L, "Rosa", 40),
                fila(4L, "Teo", 0)));

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals(
        List.of(1, 2, 2, 4), ranking.posiciones().stream().map(p -> p.puesto()).toList());
    assertTrue(ranking.posiciones().get(1).esActual());
    assertFalse(ranking.posiciones().get(0).esActual());
    assertEquals(2, ranking.miPosicion());
    assertEquals(40, ranking.miXp());
  }

  @Test
  void laSemanaVaDeLunesADomingoEnHoraDeLimaYConservaElHistorial() {
    when(repository.xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE))
        .thenReturn(List.of(fila(2L, "Luis", 0)));
    when(repository.sumarXpEntre(2L, LUNES_ANTERIOR, LUNES)).thenReturn(90);

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals(LocalDate.of(2026, 11, 9), ranking.inicioSemana());
    assertEquals(LocalDate.of(2026, 11, 15), ranking.finSemana());
    assertEquals("09/11 al 15/11", ranking.rangoSemana());
    assertEquals(0, ranking.miXp(), "La XP semanal empieza en cero cada lunes");
    assertEquals(90, ranking.xpSemanaPasada());
    assertEquals(luis.getXp(), ranking.xpTotal(), "La XP total no se reinicia");
  }

  @Test
  void elDomingoEnLaNocheSigueSiendoLaMismaSemana() {
    Instant domingoNoche = Instant.parse("2026-11-16T04:30:00Z"); // domingo 23:30 en Lima
    var servicio = new RankingService(repository, Clock.fixed(domingoNoche, ZoneOffset.UTC));
    when(repository.xpDelSalonEntre(any(), any(), any(), any())).thenReturn(List.of());

    var ranking = servicio.obtenerRankingDelSalon(2L);

    assertEquals(LocalDate.of(2026, 11, 9), ranking.inicioSemana());
    verify(repository).xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE);
  }

  @Test
  void muestraElTop10PeroCalculaElPuestoPropio() {
    List<XpDelPeriodo> salon = new ArrayList<>();
    for (long i = 1; i <= 11; i++) {
      salon.add(fila(i + 10, "Estudiante " + i, 200 - (int) i * 10));
    }
    salon.add(fila(2L, "Luis", 5));
    when(repository.xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE)).thenReturn(salon);

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals(RankingService.TAMANO_RANKING, ranking.posiciones().size());
    assertEquals(12, ranking.miPosicion());
    assertTrue(ranking.posiciones().stream().noneMatch(p -> p.esActual()));
  }

  @Test
  void laLigaDependeDeLaXpSemanalYIndicaCuantoFaltaParaSubir() {
    when(repository.xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE))
        .thenReturn(List.of(fila(1L, "Ana", 130), fila(2L, "Luis", 50)));

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals(Liga.PANACA, ranking.miLiga());
    assertEquals(Liga.AUQUI, ranking.siguienteLiga());
    assertEquals(30, ranking.xpParaSiguienteLiga());
    assertEquals(Liga.INCA, ranking.posiciones().get(0).liga());
  }

  @Test
  void usaElAvatarDeCadaEstudiante() {
    when(repository.xpDelSalonEntre(1, "A", LUNES, LUNES_SIGUIENTE))
        .thenReturn(List.of(new Fila(2L, "Luis", "PUMA", 10), new Fila(3L, "Rosa", null, 0)));

    var ranking = service.obtenerRankingDelSalon(2L);

    assertEquals("🐆", ranking.posiciones().get(0).avatar());
    assertEquals("✨", ranking.posiciones().get(1).avatar());
  }

  private static XpDelPeriodo fila(Long id, String nombre, int puntos) {
    return new Fila(id, nombre, "KAI", puntos);
  }

  private static Estudiante estudiante(Long id, String nombre, MisionInicial... misiones) {
    Estudiante estudiante = spy(new Estudiante(nombre, nombre + "@example.org", "hash", 1, "A"));
    doReturn(id).when(estudiante).getId();
    for (MisionInicial mision : misiones) {
      estudiante.registrarMision(mision);
    }
    return estudiante;
  }

  private static final class Fila implements XpDelPeriodo {
    private final Long id;
    private final String nombre;
    private final String avatar;
    private final Integer puntos;

    Fila(Long id, String nombre, String avatar, Integer puntos) {
      this.id = id;
      this.nombre = nombre;
      this.avatar = avatar;
      this.puntos = puntos;
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public String getNombre() {
      return nombre;
    }

    @Override
    public String getAvatar() {
      return avatar;
    }

    @Override
    public Integer getPuntos() {
      return puntos;
    }
  }
}