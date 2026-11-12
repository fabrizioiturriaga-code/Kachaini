package kanichi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cambios_liga")
public class CambioLiga {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Enumerated(EnumType.STRING)
  @Column(name = "liga_anterior", nullable = false, length = 20)
  private Liga ligaAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "liga_nueva", nullable = false, length = 20)
  private Liga ligaNueva;

  @Column(nullable = false)
  private boolean ascenso;

  @Column(nullable = false)
  private Instant fecha;

  @Column(nullable = false)
  private boolean notificado;

  protected CambioLiga() {}

  public CambioLiga(
      Long estudianteId, Liga ligaAnterior, Liga ligaNueva, Instant fecha, boolean notificado) {
    this.estudianteId = estudianteId;
    this.ligaAnterior = ligaAnterior;
    this.ligaNueva = ligaNueva;
    this.ascenso = ligaNueva.ordinal() > ligaAnterior.ordinal();
    this.fecha = fecha;
    this.notificado = notificado;
  }

  public String mensaje() {
    if (ascenso) {
      return "🎉 ¡Ascendiste a "
          + ligaNueva.getNombre()
          + " "
          + ligaNueva.getIcono()
          + "! Alcanzaste "
          + ligaNueva.getXpMinima()
          + " XP esta semana.";
    }
    return "Descendiste a "
        + ligaNueva.getNombre()
        + " "
        + ligaNueva.getIcono()
        + " porque la semana pasada no alcanzaste "
        + ligaAnterior.getXpMinima()
        + " XP. ¡Gana XP para volver a subir!";
  }

  public void marcarNotificado() {
    notificado = true;
  }

  public Liga getLigaAnterior() {
    return ligaAnterior;
  }

  public Liga getLigaNueva() {
    return ligaNueva;
  }

  public boolean isAscenso() {
    return ascenso;
  }
}
