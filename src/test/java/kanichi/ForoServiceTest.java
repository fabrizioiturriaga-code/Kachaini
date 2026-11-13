package kanichi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kanichi.dto.ForoSalon;
import kanichi.dto.MensajeForo;
import kanichi.model.AvatarEstudiante;
import kanichi.model.Estudiante;
import kanichi.model.PublicacionForo;
import kanichi.model.RachaDiaria;
import kanichi.model.RetoDocente;
import kanichi.model.TipoPublicacion;
import kanichi.repository.EstudianteRepository;
import kanichi.repository.PublicacionForoRepository;
import kanichi.service.ForoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ForoServiceTest {
  private static final Instant AHORA = Instant.parse("2026-11-13T15:00:00Z");

  private final PublicacionForoRepository publicaciones = mock(PublicacionForoRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final ForoService service =
      new ForoService(publicaciones, estudiantes, Clock.fixed(AHORA, ZoneOffset.UTC));

  private Estudiante ana;
  private Estudiante beto;

  @BeforeEach
  void setup() {
    ana = estudiante(1L, "Ana", 0);
    beto = estudiante(2L, "Beto", 0);
    when(estudiantes.findById(1L)).thenReturn(Optional.of(ana));
    when(estudiantes.findById(2L)).thenReturn(Optional.of(beto));
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "A"))
        .thenReturn(List.of(ana, beto));
  }

  @Test
  void publicarGuardaElMensajeEnElForoDeSuSalon() {
    service.publicar(1L, "  ¿Alguien repasó la unidad 3?  ");

    ArgumentCaptor<PublicacionForo> guardada = ArgumentCaptor.forClass(PublicacionForo.class);
    verify(publicaciones).save(guardada.capture());
    assertEquals("¿Alguien repasó la unidad 3?", guardada.getValue().getTexto());
    assertEquals(2, guardada.getValue().getGrado());
    assertEquals("A", guardada.getValue().getSeccion());
    assertEquals(1L, guardada.getValue().getAutorId());
    assertEquals(TipoPublicacion.MENSAJE, guardada.getValue().getTipo());
    assertEquals(AHORA, guardada.getValue().getCreada());
  }

  @Test
  void noSePublicaUnMensajeVacioNiMuyLargo() {
    var vacio = assertThrows(IllegalArgumentException.class, () -> service.publicar(1L, "   "));
    assertEquals("Escribe un mensaje antes de publicar.", vacio.getMessage());

    var largo =
        assertThrows(
            IllegalArgumentException.class, () -> service.publicar(1L, "a".repeat(501)));
    assertEquals("El mensaje no puede tener más de 500 caracteres.", largo.getMessage());
    verify(publicaciones, never()).save(any());
  }

  @Test
  void elForoMuestraLosMensajesDelSalonYLosAnunciosAutomaticos() {
    PublicacionForo mensaje =
        new PublicacionForo(2, "A", 1L, TipoPublicacion.MENSAJE, "Hola salón", null, AHORA);
    PublicacionForo anuncio =
        new PublicacionForo(
            2, "A", null, TipoPublicacion.RETO_SALON, "🎉 ¡El salón 2.° A completó...", "RETO:1:A",
            AHORA.minusSeconds(60));
    when(publicaciones.findTop50ByGradoAndSeccionOrderByCreadaDescIdDesc(2, "A"))
        .thenReturn(List.of(mensaje, anuncio));
    when(estudiantes.findAllById(List.of(1L))).thenReturn(List.of(ana));

    ForoSalon foro = service.verForo(2L);

    assertEquals("2.° A", foro.salon());
    assertEquals(2, foro.mensajes().size());
    MensajeForo deAna = foro.mensajes().get(0);
    assertEquals("Ana", deAna.autor());
    assertEquals("✨", deAna.avatar());
    assertEquals("Hola salón", deAna.texto());
    assertEquals("13/11 10:00", deAna.fecha());
    assertFalse(deAna.propio(), "Beto no es el autor");
    MensajeForo automatico = foro.mensajes().get(1);
    assertTrue(automatico.isAutomatico());
    assertEquals("Kaichaini", automatico.autor());
    assertTrue(service.verForo(1L).mensajes().get(0).propio(), "Ana ve su propio mensaje");
  }

  @Test
  void dosEstudiantesConRachaLargaSeAnuncianUnaVezPorSemana() {
    Estudiante carla = estudiante(3L, "Carla", 7);
    Estudiante dante = estudiante(4L, "Dante", 9);
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "A"))
        .thenReturn(List.of(ana, carla, dante));
    when(publicaciones.publicarAutomatica(
            any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    assertTrue(service.anunciarRachas(2, "A"));

    verify(publicaciones)
        .publicarAutomatica(
            2,
            "A",
            "RACHA",
            "🔥 ¡Carla y Dante llevan 7 días o más de racha! Sigan así.",
            "RACHA:2:A:" + LocalDate.of(2026, 11, 9),
            AHORA);
  }

  @Test
  void conUnSoloEstudianteEnRachaNoSeAnuncia() {
    Estudiante carla = estudiante(3L, "Carla", 7);
    when(estudiantes.findByGradoAndSeccionOrderByNombreAsc(2, "A"))
        .thenReturn(List.of(ana, beto, carla));

    assertFalse(service.anunciarRachas(2, "A"));
    verify(publicaciones, never())
        .publicarAutomatica(any(), any(), any(), any(), any(), any());
  }

  @Test
  void elRetoCompletadoSeAnunciaConUnaClavePorRetoYSeccion() {
    RetoDocente reto = mock(RetoDocente.class);
    when(reto.getId()).thenReturn(10L);
    when(reto.getTitulo()).thenReturn("Energía Inca");
    when(publicaciones.publicarAutomatica(
            eq(4), eq("A"), eq("RETO_SALON"), anyString(), eq("RETO:10:A"), eq(AHORA)))
        .thenReturn(1, 0);

    assertTrue(service.anunciarRetoCompletado(reto, 4, "A"));
    assertFalse(service.anunciarRetoCompletado(reto, 4, "A"), "No se anuncia dos veces");
    verify(publicaciones, times(2))
        .publicarAutomatica(
            4,
            "A",
            "RETO_SALON",
            "🎉 ¡El salón 4.° A completó el reto «Energía Inca»! Todos sus estudiantes lo terminaron.",
            "RETO:10:A",
            AHORA);
  }

  private static Estudiante estudiante(Long id, String nombre, int diasRacha) {
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getId()).thenReturn(id);
    when(estudiante.getNombre()).thenReturn(nombre);
    when(estudiante.getGrado()).thenReturn(2);
    when(estudiante.getSeccion()).thenReturn("A");
    when(estudiante.getAvatar()).thenReturn(AvatarEstudiante.KAI);
    when(estudiante.rachaEn(any())).thenReturn(new RachaDiaria(diasRacha, diasRacha > 0));
    return estudiante;
  }
}