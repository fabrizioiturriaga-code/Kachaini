package kanichi.model;
public enum Logro {
  PRIMEROS_PASOS(
      "Primeros pasos",
      "👣",
      "Completaste el tutorial de Kaichaini.",
      "Completa la misión de bienvenida del módulo inicial."),
  PRIMER_ACIERTO(
      "Primer acierto",
      "🎯",
      "Respondiste correctamente tu primer reto.",
      "Responde bien el reto de bienvenida del módulo inicial."),
  EXPLORADOR(
      "Explorador de Kaichaini",
      "🧭",
      "Completaste el módulo inicial.",
      "Completa todas las misiones del módulo inicial."),
  APRENDIZ_DE_HISTORIA(
      "Aprendiz de historia",
      "📜",
      "Completaste tu primera lección del mapa.",
      "Completa cualquier lección del mapa de aprendizaje."),
  UNIDAD_DOMINADA(
      "Unidad dominada",
      "🏅",
      "Completaste todas las lecciones de una unidad.",
      "Completa las " + Leccion.LECCIONES_POR_UNIDAD + " lecciones de una misma unidad."),
  CIEN_XP(
      "Cien puntos",
      "💯",
      "Alcanzaste 100 XP.",
      "Acumula 100 XP completando misiones y lecciones."),
  RACHA_DE_TRES(
      "Constancia",
      "🔥",
      "Mantuviste una racha de 3 días.",
      "Completa al menos una lección durante 3 días seguidos."),
  MAESTRO_DEL_GRADO(
      "Maestro del grado",
      "🏆",
      "Completaste todas las lecciones de tu grado.",
      "Completa todas las unidades del mapa de tu grado."),
  PRACTICA_DESTACADA(
      "Estudiante destacado",
      "🌟",
      "Obtuviste 80 % o más en una práctica.",
      "Obtén 80 % o más de aciertos en una lectura o actividad de arrastre."),
  TRABAJO_EN_EQUIPO(
      "Trabajo en equipo",
      "🤝",
      "Completaste un desafío con un compañero.",
      "Completa un desafío entre estudiantes.");

  /** XP necesaria para {@link #CIEN_XP}. */
  public static final int XP_PARA_CIEN_XP = 100;

  /** Días de racha necesarios para {@link #RACHA_DE_TRES}. */
  public static final int DIAS_PARA_CONSTANCIA = 3;

  private final String nombre;
  private final String icono;
  private final String descripcion;
  private final String comoConseguirlo;

  Logro(String nombre, String icono, String descripcion, String comoConseguirlo) {
    this.nombre = nombre;
    this.icono = icono;
    this.descripcion = descripcion;
    this.comoConseguirlo = comoConseguirlo;
  }

  public String getNombre() {
    return nombre;
  }

  public String getIcono() {
    return icono;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getComoConseguirlo() {
    return comoConseguirlo;
  }
}
