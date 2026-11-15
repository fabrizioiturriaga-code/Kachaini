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
import org.springframework.web.server.ResponseStatusException;
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

    private Estudiante ana;
    private Notificacion notificacion;

    @BeforeEach
    void preparar() {
      ana = mock(Estudiante.class);
      when(ana.getId()).thenReturn(1L);
      when(ana.getGrado()).thenReturn(2);
      when(ana.getSeccion()).thenReturn("A");
      notificacion = new Notificacion(1L, 10L, ahora);
      when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
      when(estudiantes.bloquearSalon(2, "A")).thenReturn(List.of(ana));
      when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L))
          .thenReturn(Optional.of(notificacion));
    }

    @Test
    @DisplayName("C1: D1 verdadero -> el estudiante no existe (404)")
    void c1EstudianteNoExiste() {
      when(estudiantes.findById(1L)).thenReturn(Optional.empty());

      assertThrows(ResponseStatusException.class, () -> service.completar(1L, 10L, null));
      verifyNoInteractions(retos, foroService);
    }

    @Test
    @DisplayName("C2: D2 verdadero -> el reto no se le notificó al estudiante (404)")
    void c2SinNotificacion() {
      when(notificaciones.findByEstudianteIdAndRetoId(1L, 10L)).thenReturn(Optional.empty());

      assertThrows(ResponseStatusException.class, () -> service.completar(1L, 10L, null));
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
    }

    @Test
    @DisplayName("C3: D3 verdadero -> el reto ya estaba completado")
    void c3YaCompletado() {
      notificacion.completar(ahora);
      usarReto(reto(hoy.plusDays(5), 20));

      assertError("Ya completaste este reto.", () -> service.completar(1L, 10L, null));
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
    }

    @Test
    @DisplayName("C4: D4 verdadero -> ya falló la pregunta, no puede volver a intentar")
    void c4YaFallo() {
      notificacion.registrarFallo(ahora);
      usarReto(retoConPregunta());

      assertError(
          "Ya respondiste este reto y tu respuesta no fue correcta.",
          () -> service.completar(1L, 10L, 1));
      assertFalse(notificacion.isCompletada());
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
    }

    @Test
    @DisplayName("C5: D5 verdadero -> el reto está vencido")
    void c5RetoVencido() {
      usarReto(reto(hoy.minusDays(1), 20));

      assertError("Este reto ya venció.", () -> service.completar(1L, 10L, null));
      assertFalse(notificacion.isCompletada());
    }

    @Test
    @DisplayName("C6: D6 y D7 verdaderos -> no eligió respuesta")
    void c6SinRespuesta() {
      usarReto(retoConPregunta());

      assertError("Elige una respuesta antes de enviar.", () -> service.completar(1L, 10L, null));
      assertFalse(notificacion.isFallida());
      assertFalse(notificacion.isCompletada());
    }

    @Test
    @DisplayName("C7: D6 y D8 verdaderos -> respuesta incorrecta, gasta el único intento")
    void c7RespuestaIncorrecta() {
      usarReto(retoConPregunta());

      ResultadoReto resultado = service.completar(1L, 10L, 0);

      assertFalse(resultado.correcto());
      assertTrue(notificacion.isFallida());
      assertFalse(notificacion.isCompletada());
      verify(ana, never()).sumarXp(anyInt());
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
      verifyNoInteractions(foroService);
    }

    @Test
    @DisplayName("C8: D6 verdadero, D8 falso -> respuesta correcta, gana XP sin anuncio")
    void c8RespuestaCorrecta() {
      usarReto(retoConPregunta());
      when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(1L);

      ResultadoReto resultado = service.completar(1L, 10L, 1);

      assertTrue(resultado.correcto());
      assertEquals(20, resultado.xp());
      assertTrue(notificacion.isCompletada());
      verify(estudiantes).registrarXp(1L, 20, ahora);
      verifyNoInteractions(foroService);
    }

    @Test
    @DisplayName("C9: D6 falso, D9 falso -> reto sin recompensa, no registra XP")
    void c9RetoSinXp() {
      usarReto(reto(hoy.plusDays(5), 0));
      when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(1L);

      ResultadoReto resultado = service.completar(1L, 10L, null);

      assertEquals(0, resultado.xp());
      assertTrue(notificacion.isCompletada());
      verify(ana, never()).sumarXp(anyInt());
      verify(estudiantes, never()).registrarXp(any(), anyInt(), any());
    }

    @Test
    @DisplayName("C10: D9 verdadero, D10 falso -> gana XP y no se anuncia en el foro")
    void c10FaltanCompaneros() {
      usarReto(reto(hoy.plusDays(5), 20));
      when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(2L);

      ResultadoReto resultado = service.completar(1L, 10L, null);

      assertEquals(20, resultado.xp());
      assertFalse(resultado.salonCompleto());
      verify(estudiantes).registrarXp(1L, 20, ahora);
      verifyNoInteractions(foroService);
    }

    @Test
    @DisplayName("C11: D9 y D10 verdaderos -> todo el salón terminó y se anuncia en el foro")
    void c11SalonCompleto() {
      RetoDocente reto = reto(hoy.plusDays(5), 20);
      usarReto(reto);
      when(notificaciones.contarPendientesDelSalon(10L, 2, "A")).thenReturn(0L);
      when(foroService.anunciarRetoCompletado(reto, 2, "A")).thenReturn(true);

      ResultadoReto resultado = service.completar(1L, 10L, null);

      assertTrue(resultado.salonCompleto());
      verify(foroService).anunciarRetoCompletado(reto, 2, "A");
    }

    private void usarReto(RetoDocente reto) {
      when(retos.findById(10L)).thenReturn(Optional.of(reto));
    }

    private RetoDocente reto(LocalDate fechaLimite, int xp) {
      RetoDocente reto =
          spy(new RetoDocente(9L, 2, "A", "Reto 10", "Desc", "Objetivo", fechaLimite, xp, ahora));
      doReturn(10L).when(reto).getId();
      return reto;
    }

    private RetoDocente retoConPregunta() {
      RetoDocente reto = reto(hoy.plusDays(5), 20);
      reto.agregarPregunta(
          "¿Quién fundó el Tahuantinsuyo?", List.of("Manco Cápac", "Pachacútec", "Atahualpa"), 1);
      return reto;
    }

    private void assertError(String mensaje, Runnable accion) {
      var error = assertThrows(IllegalArgumentException.class, accion::run);
      assertEquals(mensaje, error.getMessage());
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