package kanichi.dto;

public record ResultadoReto(int xp, boolean salonCompleto, boolean correcto) {

  public ResultadoReto(int xp, boolean salonCompleto) {
    this(xp, salonCompleto, true);
  }

  public static ResultadoReto incorrecto() {
    return new ResultadoReto(0, false, false);
  }

  public String mensaje() {
    if (!correcto) {
      return "Respuesta incorrecta. Solo había un intento, así que este reto ya no se puede completar.";
    }
    StringBuilder texto = new StringBuilder("¡Reto completado!");
    if (xp > 0) {
      texto.append(" Ganaste ").append(xp).append(" XP.");
    }
    if (salonCompleto) {
      texto.append(" Todo tu salón lo completó: lo anunciamos en el foro 🎉");
    }
    return texto.toString();
  }
}