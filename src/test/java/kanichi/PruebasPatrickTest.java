package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.dto.ResultadoReto;
import kanichi.model.Estudiante;
import kanichi.model.MisionInicial;
import kanichi.model.Notificacion;
import kanichi.model.RetoDocente;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.EstudianteRepository.XpDelPeriodo;
import kanichi.repository.NotificacionRepository;
import kanichi.repository.RetoDocenteRepository;
import kanichi.service.ForoService;
import kanichi.service.RankingService;
import kanichi.service.RetoDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Pruebas Patrick")
class PruebasPatrickTest {
  @Nested
  @DisplayName("Caja blanca - HU-15 completar reto")
  class CajaBlanca {
    private final Instant ahora = Instant.parse("2026-11-12T18:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 11, 12);

    private final RetoDocenteRepository retos = mock(RetoDocenteRepository.class);
    private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
    private final ForoService foroService = mock(ForoService.class);
    private final RetoDocenteService service =
        new RetoDocenteService(
            retos, notificaciones, estudiantes, foroService, Clock.fixed(ahora, ZoneOffset.UTC));

    @Test
    @DisplayName("Una respuesta incorrecta gasta el único intento")
    void unaRespuestaIncorrectaGastaElUnicoIntento() {
      Estudiante ana = mock(Estudiante.class);
      when(ana.getId()).thenReturn(1L);
      when(ana.getGrado()).thenReturn(2);
      when(ana.getSeccion()).thenReturn("A");
      Notificacion notificacion = new Notificacion(1L, 10L, ahora);
      RetoDocente reto =
          spy(new RetoDocente(9L, 2, "A", "Reto 10", "Desc", "Objetivo", hoy.plusDays(5), 20, ahora));
      doReturn(10L).when(reto).getId();
      reto.agregarPregunta(
          "¿Quién fundó el Tahuantinsuyo?", List.of("Manco Cápac", "Pachacútec", "Atahualpa"), 1);
      when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
      when(estudiantes.bloquearSalon(2, "A")).thenReturn(List.of(ana));
      when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
          .thenReturn(Optional.of(notificacion));
      when(retos.findById(10L)).thenReturn(Optional.of(reto));

      ResultadoReto resultado = service.completar(1L, 10L, 0);

      assertFalse(resultado.correcto());
      assertTrue(notificacion.isFallida());
      assertFalse(notificacion.isCompletada());

      var error =
          assertThrows(IllegalArgumentException.class, () -> service.completar(1L, 10L, 1));
      assertEquals("Ya respondiste este reto y tu respuesta no fue correcta.", error.getMessage());

      verify(ana, never()).sumarXp(anyInt());
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
      verifyNoInteractions(foroService);
    }
  }
  @Nested
  @DisplayName("Unitaria - HU-14 ranking semanal")
  class Unitaria {
    private final Instant jueves = Instant.parse("2026-11-12T18:00:00Z");
    private final Instant lunes = Instant.parse("2026-11-09T05:00:00Z");
    private final Instant lunesSiguiente = Instant.parse("2026-11-16T05:00:00Z");

    private final EstudianteRepository repository = mock(EstudianteRepository.class);
    private final RankingService service =
        new RankingService(repository, Clock.fixed(jueves, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
      Estudiante luis = spy(new Estudiante("Luis", "Luis@example.org", "hash", 1, "A"));
      doReturn(2L).when(luis).getId();
      luis.registrarMision(MisionInicial.BIENVENIDA);
      luis.registrarMision(MisionInicial.QUE_ES_LA_HISTORIA);
      when(repository.findById(2L)).thenReturn(Optional.of(luis));
    }

    @Test
    @DisplayName("Ordena por XP de la semana y los empates comparten puesto")
    void ordenaPorXpDeLaSemanaYLosEmpatesCompartenPuesto() {
      when(repository.xpDelSalonEntre(1, "A", lunes, lunesSiguiente))
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

    private XpDelPeriodo fila(Long id, String nombre, int puntos) {
      return new XpDelPeriodo() {
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
          return "KAI";
        }

        @Override
        public Integer getPuntos() {
          return puntos;
        }
      };
    }
  }
}