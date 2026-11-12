package kanichi.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Lecciones de cada unidad del mapa de aprendizaje, en el orden en que deben completarse. Cada
 * unidad tiene {@link #LECCIONES_POR_UNIDAD} lecciones; completar todas la marca como completada y
 * desbloquea la siguiente (HU-18). Cada lección tiene una pregunta: un error resta energía (HU-12).
 */
public enum Leccion {
  U1_PRIMEROS_POBLADORES(
      "primeros-pobladores",
      1,
      "Los primeros pobladores",
      List.of(
          "Hace más de 15 000 años los primeros grupos humanos llegaron a América. La explicación"
              + " más aceptada dice que venían de Asia y cruzaron por la zona del estrecho de"
              + " Bering.",
          "Eran grupos nómadas: se movían de un lugar a otro para cazar animales y recolectar"
              + " frutos y raíces."),
      20,
      new Pregunta(
          "¿Cómo vivían los primeros pobladores de América?",
          List.of(
              "En grandes ciudades con templos de piedra",
              "Como nómadas que cazaban y recolectaban",
              "Cultivando campos en terrazas"),
          1,
          "Todavía no conocían la agricultura: se desplazaban para encontrar alimento.",
          "Fíjate en cómo conseguían alimento y si podían quedarse siempre en el mismo lugar."),
      "Explica con tus propias palabras por qué los primeros pobladores se desplazaban de un lugar"
          + " a otro."),
  U1_CAZADORES_DEL_PERU(
      "cazadores-del-peru",
      1,
      "Cazadores y recolectores en el Perú",
      List.of(
          "En el Perú se han encontrado restos de estos primeros habitantes en cuevas como"
              + " Lauricocha (Huánuco), Pikimachay (Ayacucho) y Toquepala (Tacna).",
          "En Toquepala hay pinturas rupestres que muestran cómo cazaban camélidos silvestres,"
              + " como la vicuña y el guanaco."),
      20,
      new Pregunta(
          "¿Qué muestran las pinturas rupestres de Toquepala?",
          List.of(
              "Batallas contra los españoles",
              "Cultivos de maíz en terrazas",
              "Escenas de caza de camélidos"),
          2,
          "Las pinturas rupestres son un testimonio de la caza en esa época.",
          "Busca qué actividad representaban los dibujos y qué animales aparecen.")),
  U1_AGRICULTURA(
      "agricultura-y-aldeas",
      1,
      "La agricultura y la vida en aldeas",
      List.of(
          "Hace miles de años los antiguos peruanos aprendieron a cultivar plantas como la papa, el"
              + " maíz y el algodón, y a criar llamas y alpacas.",
          "Al tener comida cerca de sus campos, dejaron de moverse constantemente y formaron"
              + " aldeas."),
      20,
      new Pregunta(
          "¿Qué cambio trajo la agricultura?",
          List.of(
              "Las personas se establecieron en aldeas",
              "Las personas dejaron de usar herramientas",
              "Desaparecieron los animales domésticos"),
          0,
          "Con la agricultura el alimento se producía en un lugar fijo y nacieron las aldeas.",
          "Piensa qué pasó cuando las familias pudieron conseguir alimento cerca de sus campos.")),
  U2_CARAL(
      "caral",
      2,
      "Caral, la civilización más antigua",
      List.of(
          "Caral es la ciudad más antigua de América. Se construyó hacia el 3000 a. C. en el valle"
              + " de Supe, en la costa central del Perú.",
          "Sus pirámides, plazas y templos muestran una sociedad organizada que vivía de la"
              + " agricultura y el intercambio."),
      20,
      new Pregunta(
          "¿Dónde se encuentra la ciudad de Caral?",
          List.of(
              "En el valle de Supe, en la costa central",
              "En el Cusco, cerca de Machu Picchu",
              "A orillas del lago Titicaca"),
          0,
          "Caral está en el valle de Supe, a unos 180 km al norte de Lima.",
          "Busca en el primer párrafo el nombre del valle donde se levantó la ciudad.")),
  U2_CHAVIN_PARACAS(
      "chavin-paracas",
      2,
      "Chavín y Paracas",
      List.of(
          "Chavín de Huántar (Áncash) fue un gran centro religioso. Allí se encuentra el Lanzón,"
              + " una escultura de piedra que representaba a su dios principal.",
          "La cultura Paracas, en la costa sur, es famosa por sus mantos funerarios bordados con"
              + " colores muy vivos."),
      20,
      new Pregunta(
          "¿Por qué es famosa la cultura Paracas?",
          List.of(
              "Por la ciudadela de Machu Picchu",
              "Por sus mantos funerarios bordados",
              "Por la ciudad de adobe de Chan Chan"),
          1,
          "Los mantos de Paracas son una de las mejores muestras textiles del mundo antiguo.",
          "Revisa qué objetos dejaron en sus entierros y cómo estaban decorados.")),
  U2_NAZCA_MOCHE_CHIMU(
      "nazca-moche-chimu",
      2,
      "Nazca, Moche y Chimú",
      List.of(
          "Los nazca dibujaron enormes figuras en la pampa de Ica, conocidas como las líneas de"
              + " Nazca. Los moche levantaron las huacas del Sol y de la Luna y enterraron al Señor"
              + " de Sipán.",
          "Los chimú construyeron Chan Chan, en La Libertad, la mayor ciudad de adobe de"
              + " América."),
      20,
      new Pregunta(
          "¿Qué cultura construyó la ciudad de Chan Chan?",
          List.of("Los nazca", "Los moche", "Los chimú"),
          2,
          "Chan Chan fue la capital del reino chimú.",
          "Distingue a quienes trazaron figuras en la pampa de quienes levantaron la ciudad de"
              + " adobe.")),
  U3_ORIGEN_Y_EXPANSION(
      "origen-tahuantinsuyo",
      3,
      "Origen y expansión del Tahuantinsuyo",
      List.of(
          "Los incas fundaron su capital en el Cusco. Con gobernantes como Pachacútec, en el siglo"
              + " XV, ampliaron su territorio hasta formar un gran imperio.",
          "Tahuantinsuyo significa «las cuatro regiones unidas»: Chinchaysuyo, Antisuyo, Collasuyo"
              + " y Contisuyo."),
      20,
      new Pregunta(
          "¿Qué significa Tahuantinsuyo?",
          List.of("La tierra del sol", "El camino largo", "Las cuatro regiones unidas"),
          2,
          "El imperio estaba dividido en cuatro suyos que se unían en el Cusco.",
          "Observa cuántos territorios se nombran y cómo se organizaban alrededor del Cusco.")),
  U3_AYNI_MINKA_MITA(
      "ayni-minka-mita",
      3,
      "Ayni, minka y mita",
      List.of(
          "El ayni era la ayuda mutua entre familias: hoy te ayudo y mañana tú me ayudas.",
          "La minka era el trabajo de toda la comunidad en obras que beneficiaban a todos, y la"
              + " mita era el trabajo por turnos para el Estado inca."),
      20,
      new Pregunta(
          "¿Cómo se llamaba la ayuda mutua entre familias?",
          List.of("Ayni", "Mita", "Quipu"),
          0,
          "El ayni se basa en la reciprocidad: ayudar para ser ayudado después.",
          "Busca la práctica en la que una familia devuelve la ayuda que recibió.")),
  U3_CAMINOS_Y_QUIPUS(
      "caminos-y-quipus",
      3,
      "Caminos y quipus",
      List.of(
          "El Qhapaq Ñan era una enorme red de caminos que unía todo el imperio. Por ellos corrían"
              + " los chasquis, mensajeros que llevaban noticias.",
          "Los incas registraban información, como cantidades de productos, con los quipus: cuerdas"
              + " con nudos de distintos tipos y colores."),
      20,
      new Pregunta(
          "¿Para qué se usaban los quipus?",
          List.of(
              "Para pintar las paredes de los templos",
              "Para registrar información con cuerdas y nudos",
              "Para cruzar los ríos"),
          1,
          "Los quipus eran un sistema de registro hecho con cuerdas y nudos.",
          "Distingue la red de caminos del objeto que usaba nudos para llevar cuentas.")),
  U4_LLEGADA_ESPANOLES(
      "llegada-de-los-espanoles",
      4,
      "La llegada de los españoles",
      List.of(
          "En 1532 el conquistador Francisco Pizarro llegó al Tahuantinsuyo con un pequeño grupo de"
              + " soldados.",
          "En ese momento el imperio estaba debilitado por una guerra entre dos hermanos, Huáscar y"
              + " Atahualpa, que se disputaban el poder."),
      20,
      new Pregunta(
          "¿Quién dirigió la expedición española que llegó en 1532?",
          List.of("Francisco Pizarro", "Simón Bolívar", "José de San Martín"),
          0,
          "Pizarro lideró la conquista del Tahuantinsuyo.",
          "Busca a la persona que llegó con la expedición y distínguela de los hermanos incas.")),
  U4_ATAHUALPA_EN_CAJAMARCA(
      "atahualpa-en-cajamarca",
      4,
      "La captura de Atahualpa",
      List.of(
          "En noviembre de 1532, en Cajamarca, los españoles capturaron al inca Atahualpa.",
          "Atahualpa ofreció como rescate una habitación llena de oro y dos de plata, pero fue"
              + " ejecutado en 1533."),
      20,
      new Pregunta(
          "¿En qué ciudad fue capturado Atahualpa?",
          List.of("Cusco", "Cajamarca", "Lima"),
          1,
          "La captura ocurrió en Cajamarca, en 1532.",
          "Revisa en qué lugar se produjo el encuentro que terminó con la captura del inca.")),
  U4_RESISTENCIA_INCA(
      "resistencia-inca",
      4,
      "La resistencia inca",
      List.of(
          "Manco Inca se rebeló contra los españoles en 1536 y sitió el Cusco. Después se retiró a"
              + " Vilcabamba, donde se mantuvo un estado inca independiente.",
          "Ese estado resistió hasta 1572, cuando fue ejecutado el inca Túpac Amaru I."),
      20,
      new Pregunta(
          "¿Qué inca lideró la gran rebelión de 1536?",
          List.of("Pachacútec", "Huáscar", "Manco Inca"),
          2,
          "Manco Inca organizó el sitio del Cusco y la resistencia en Vilcabamba.",
          "Busca quién organizó el sitio del Cusco antes de retirarse a Vilcabamba.")),
  U5_VIRREINATO(
      "el-virreinato",
      5,
      "El Virreinato del Perú",
      List.of(
          "Tras la conquista, España organizó el Virreinato del Perú, con capital en Lima, la"
              + " «Ciudad de los Reyes» fundada por Pizarro en 1535.",
          "El virrey gobernaba en nombre del rey de España."),
      20,
      new Pregunta(
          "¿Quién gobernaba el virreinato en nombre del rey de España?",
          List.of("El virrey", "El curaca", "El chasqui"),
          0,
          "El virrey era el representante del rey en el territorio.",
          "Piensa en quién representaba al rey directamente en el territorio.")),
  U5_ECONOMIA_COLONIAL(
      "economia-colonial",
      5,
      "Minería y economía colonial",
      List.of(
          "La riqueza del virreinato venía sobre todo de la minería: la plata de Potosí y el"
              + " mercurio de Huancavelica.",
          "Para trabajar en las minas se usó la mita minera, que obligaba a muchos indígenas a"
              + " trabajar por turnos en condiciones muy duras."),
      20,
      new Pregunta(
          "¿Qué metal fue la base de la riqueza del virreinato?",
          List.of("El hierro", "La plata", "El aluminio"),
          1,
          "La plata de Potosí fue el principal producto de exportación.",
          "Revisa el producto extraído en Potosí que se enviaba a España.")),
  U5_SOCIEDAD_COLONIAL(
      "sociedad-colonial",
      5,
      "Sociedad y cultura colonial",
      List.of(
          "La sociedad estaba dividida en grupos: españoles, criollos, mestizos, indígenas y"
              + " afrodescendientes, con derechos muy distintos.",
          "En 1551 se fundó en Lima la Universidad de San Marcos, la más antigua de América."),
      20,
      new Pregunta(
          "¿Cómo se llamaba a los hijos de españoles nacidos en América?",
          List.of("Mestizos", "Criollos", "Curacas"),
          1,
          "Los criollos eran descendientes de españoles nacidos en América.",
          "Busca el nombre del grupo que compartía ascendencia española, pero había nacido en"
              + " América.")),
  U6_REBELIONES(
      "tupac-amaru-ii",
      6,
      "Las rebeliones y Túpac Amaru II",
      List.of(
          "Durante el virreinato hubo muchas protestas contra los abusos. La más grande fue la"
              + " rebelión de 1780, en el Cusco.",
          "La lideró José Gabriel Condorcanqui, conocido como Túpac Amaru II."),
      20,
      new Pregunta(
          "¿Quién lideró la gran rebelión de 1780?",
          List.of("Túpac Amaru II", "Atahualpa", "Francisco Pizarro"),
          0,
          "Túpac Amaru II encabezó la rebelión más importante antes de la independencia.",
          "Busca el nombre con el que se conocía a José Gabriel Condorcanqui.")),
  U6_EXPEDICION_LIBERTADORA(
      "expedicion-libertadora",
      6,
      "La Expedición Libertadora",
      List.of(
          "En 1820 llegó al Perú la Expedición Libertadora, dirigida por José de San Martín.",
          "El 28 de julio de 1821, en la plaza mayor de Lima, San Martín proclamó la independencia"
              + " del Perú."),
      20,
      new Pregunta(
          "¿Quién proclamó la independencia del Perú el 28 de julio de 1821?",
          List.of("Simón Bolívar", "Túpac Amaru II", "José de San Martín"),
          2,
          "San Martín proclamó la independencia en Lima.",
          "Distingue quién proclamó la independencia en Lima de quienes continuaron la campaña.")),
  U6_AYACUCHO(
      "ayacucho",
      6,
      "La batalla de Ayacucho",
      List.of(
          "Aunque se proclamó la independencia, aún quedaban ejércitos realistas. Simón Bolívar y"
              + " Antonio José de Sucre continuaron la lucha.",
          "El 9 de diciembre de 1824, en la batalla de Ayacucho, el ejército patriota derrotó a los"
              + " realistas y se aseguró la independencia."),
      20,
      new Pregunta(
          "¿Qué batalla consolidó la independencia del Perú en 1824?",
          List.of("La batalla de Ayacucho", "La batalla de Cajamarca", "La batalla de Lima"),
          0,
          "Ayacucho fue la batalla decisiva que selló la independencia.",
          "Busca el enfrentamiento de diciembre de 1824 que puso fin a la resistencia realista.")),
  ;

  /** Número de lecciones que tiene cada unidad. */
  public static final int LECCIONES_POR_UNIDAD = 3;

  public record Pregunta(
      String enunciado,
      List<String> opciones,
      int respuestaCorrecta,
      String explicacion,
      String pista) {}

  private final String codigo;
  private final int unidad;
  private final String titulo;
  private final List<String> contenido;
  private final int xp;
  private final Pregunta pregunta;
  private final String preguntaDesarrollo;

  Leccion(
      String codigo, int unidad, String titulo, List<String> contenido, int xp, Pregunta pregunta) {
    this(codigo, unidad, titulo, contenido, xp, pregunta, null);
  }

  Leccion(
      String codigo,
      int unidad,
      String titulo,
      List<String> contenido,
      int xp,
      Pregunta pregunta,
      String preguntaDesarrollo) {
    this.codigo = codigo;
    this.unidad = unidad;
    this.titulo = titulo;
    this.contenido = contenido;
    this.xp = xp;
    this.pregunta = pregunta;
    this.preguntaDesarrollo = preguntaDesarrollo;
  }

  public static Optional<Leccion> porCodigo(String codigo) {
    return Arrays.stream(values()).filter(l -> l.codigo.equals(codigo)).findFirst();
  }

  public static List<Leccion> deUnidad(int unidad) {
    return Arrays.stream(values()).filter(l -> l.unidad == unidad).toList();
  }

  /** Lección que debe completarse antes que esta, si existe. */
  public Optional<Leccion> anterior() {
    return ordinal() == 0 ? Optional.empty() : Optional.of(values()[ordinal() - 1]);
  }

  public boolean aceptaRespuesta(Integer respuesta) {
    return respuesta != null && respuesta == pregunta.respuestaCorrecta();
  }

  public String getCodigo() {
    return codigo;
  }

  public int getUnidad() {
    return unidad;
  }

  public String getTitulo() {
    return titulo;
  }

  public List<String> getContenido() {
    return contenido;
  }

  public int getXp() {
    return xp;
  }

  public String getPreguntaDesarrollo() {
    return preguntaDesarrollo;
  }

  public Pregunta getPregunta() {
    return pregunta;
  }
}
