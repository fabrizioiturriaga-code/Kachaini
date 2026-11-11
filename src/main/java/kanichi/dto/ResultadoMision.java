package kanichi.dto;

import java.util.List;
import kanichi.model.Logro;

/** Recompensas obtenidas al completar una misión. xpGanada es 0 si ya estaba completada. */
public record ResultadoMision(int xpGanada, List<Logro> logrosNuevos, boolean moduloCompletado) {}
