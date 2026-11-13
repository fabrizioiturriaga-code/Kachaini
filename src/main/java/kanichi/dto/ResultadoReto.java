package kanichi.dto;

public record ResultadoReto(int xp, boolean salonCompleto) {
  public String mensaje() {
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