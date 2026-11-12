package kanichi.dto;

import java.util.List;
import kanichi.model.Logro;

public record ResultadoMision(int xpGanada, List<Logro> logrosNuevos, boolean moduloCompletado) {}
