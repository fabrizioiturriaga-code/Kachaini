package kanichi.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Lectura {
  HOMBRE_DE_LAURICOCHA(
      "hombre-de-lauricocha",
      1,
      "El hombre de Lauricocha",
      List.of(
          "En 1958 el arqueólogo Augusto Cardich exploró unas cuevas cerca de la laguna de"
              + " Lauricocha, en Huánuco, a casi 4000 metros de altura.",
          "Allí encontró restos humanos de hace más de 9000 años, junto con puntas de piedra que"
              + " servían para cazar.",
          "Estos cazadores seguían a las manadas de vicuñas, guanacos y venados. En las épocas de"
              + " frío se refugiaban en las cuevas y usaban la piel de los animales para"
              + " abrigarse."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿De qué trata principalmente el texto?",
              List.of(
                  "De la fundación de la ciudad de Huánuco",
                  "De cómo vivían los antiguos cazadores de Lauricocha",
                  "De la crianza de llamas en la costa"),
              1,
              "El texto cuenta el hallazgo de Lauricocha y la forma de vida de sus cazadores."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Quién exploró las cuevas de Lauricocha?",
              List.of("Augusto Cardich", "Julio C. Tello", "Ruth Shady"),
              0,
              "El primer párrafo menciona al arqueólogo Augusto Cardich."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Para qué servían las puntas de piedra encontradas?",
              List.of("Para cultivar la tierra", "Para construir templos", "Para cazar animales"),
              2,
              "El texto dice que las puntas de piedra servían para cazar."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que estos cazadores usaban las cuevas?",
              List.of(
                  "Porque necesitaban protegerse del frío",
                  "Porque allí guardaban sus cosechas",
                  "Porque eran templos religiosos"),
              0,
              "Vivían a gran altura y en las épocas de frío las cuevas les daban refugio."))),
  CARAL_CIUDAD_SAGRADA(
      "caral-ciudad-sagrada",
      2,
      "Caral, ciudad sagrada",
      List.of(
          "Caral se encuentra en el valle de Supe, en la provincia de Barranca, al norte de Lima."
              + " Tiene cerca de 5000 años de antigüedad.",
          "La arqueóloga Ruth Shady dirigió las investigaciones que mostraron su importancia. En"
              + " la ciudad hay pirámides, plazas circulares hundidas y viviendas.",
          "Sus habitantes cultivaban algodón, zapallo y frijol, e intercambiaban productos con los"
              + " pescadores de la costa, que les entregaban anchoveta y mariscos."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿Cuál es la idea principal del texto?",
              List.of(
                  "Caral fue la capital de los incas",
                  "Caral era un puerto de pescadores españoles",
                  "Caral fue una antigua ciudad organizada en el valle de Supe"),
              2,
              "Todo el texto describe cómo era y cómo vivía la antigua ciudad de Caral."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Quién dirigió las investigaciones en Caral?",
              List.of("Augusto Cardich", "Ruth Shady", "Max Uhle"),
              1,
              "El segundo párrafo nombra a la arqueóloga Ruth Shady."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué recibían de los pescadores de la costa?",
              List.of("Anchoveta y mariscos", "Oro y plata", "Maíz y papa"),
              0,
              "Los pescadores entregaban anchoveta y mariscos a cambio de otros productos."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Qué muestra el intercambio entre agricultores y pescadores?",
              List.of(
                  "Que no se conocían entre sí",
                  "Que vivían en guerra constante",
                  "Que se necesitaban y colaboraban"),
              2,
              "Cada grupo daba lo que producía y recibía lo que le faltaba."))),
  LOS_CHASQUIS(
      "los-chasquis",
      3,
      "Los chasquis",
      List.of(
          "En el Tahuantinsuyo los mensajes viajaban a pie. De eso se encargaban los chasquis,"
              + " jóvenes corredores que esperaban en pequeños puestos ubicados a lo largo del"
              + " Qhapaq Ñan.",
          "Cada chasqui corría un tramo corto y entregaba el mensaje al siguiente. Así, una"
              + " noticia podía llegar del Cusco a la costa en pocos días.",
          "Para avisar su llegada tocaban un pututo, una trompeta hecha con una concha marina."
              + " Además del mensaje hablado, a veces llevaban quipus."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿De qué trata el texto?",
              List.of(
                  "De cómo se construían los templos incas",
                  "De cómo se enviaban mensajes en el Tahuantinsuyo",
                  "De los juegos de los niños incas"),
              1,
              "El texto explica el trabajo de los chasquis para llevar mensajes."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué instrumento tocaban los chasquis al llegar?",
              List.of("Una quena", "Un tambor", "Un pututo"),
              2,
              "El pututo era una trompeta hecha con una concha marina."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Por dónde corrían los chasquis?",
              List.of(
                  "Por el Qhapaq Ñan",
                  "Por los ríos de la Amazonía",
                  "Por caminos construidos por los españoles"),
              0,
              "Los puestos de los chasquis estaban a lo largo del Qhapaq Ñan."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué cada chasqui corría solo un tramo corto?",
              List.of(
                  "Porque no conocían el camino completo",
                  "Para que el mensaje avanzara rápido sin que se cansaran",
                  "Porque estaba prohibido salir de su pueblo"),
              1,
              "Al relevarse, siempre corría alguien descansado y el mensaje no se detenía."))),
  ENCUENTRO_EN_CAJAMARCA(
      "encuentro-en-cajamarca",
      4,
      "El encuentro en Cajamarca",
      List.of(
          "En noviembre de 1532, Atahualpa descansaba en los baños termales de Cajamarca después"
              + " de vencer a su hermano Huáscar en la guerra por el poder.",
          "Francisco Pizarro llegó a la ciudad con menos de 200 hombres y lo invitó a reunirse en"
              + " la plaza. Atahualpa acudió acompañado de miles de servidores, la mayoría sin"
              + " armas.",
          "Los españoles, escondidos en los edificios, atacaron por sorpresa con caballos y armas"
              + " de fuego, y capturaron al inca. Ese hecho marcó el inicio de la conquista del"
              + " Tahuantinsuyo."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿Qué hecho principal narra el texto?",
              List.of(
                  "La captura de Atahualpa en Cajamarca",
                  "La fundación de Lima",
                  "La batalla de Ayacucho"),
              0,
              "El texto narra cómo los españoles capturaron a Atahualpa en Cajamarca."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Con cuántos hombres llegó Pizarro a Cajamarca?",
              List.of("Con más de 5000", "Con unos 1000", "Con menos de 200"),
              2,
              "El segundo párrafo dice que llegó con menos de 200 hombres."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué hacía Atahualpa cuando llegaron los españoles?",
              List.of(
                  "Viajaba al Cusco",
                  "Descansaba en los baños termales",
                  "Construía un templo"),
              1,
              "Atahualpa descansaba en los baños termales de Cajamarca."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué la sorpresa fue importante para los españoles?",
              List.of(
                  "Porque eran pocos y necesitaban ventaja",
                  "Porque eran muchos más que los incas",
                  "Porque los incas los esperaban armados"),
              0,
              "Con tan pocos hombres, el ataque por sorpresa les dio la ventaja."))),
  CIUDAD_DE_LOS_REYES(
      "ciudad-de-los-reyes",
      5,
      "Lima, Ciudad de los Reyes",
      List.of(
          "Francisco Pizarro fundó Lima el 18 de enero de 1535 con el nombre de Ciudad de los"
              + " Reyes. Eligió un lugar cerca del mar y del río Rímac.",
          "Durante el virreinato, Lima fue la ciudad más importante de América del Sur: allí vivía"
              + " el virrey y, desde el puerto del Callao, salían los barcos con plata hacia"
              + " España.",
          "Sus calles se trazaron como un tablero de ajedrez, con la plaza mayor en el centro. A"
              + " su alrededor se construyeron la catedral, el palacio del virrey y el cabildo."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿Cuál es la idea principal del texto?",
              List.of(
                  "Lima fue la capital del Tahuantinsuyo",
                  "Lima fue fundada por Pizarro y fue la ciudad más importante del virreinato",
                  "Lima se fundó después de la independencia"),
              1,
              "El texto cuenta la fundación de Lima y su importancia en el virreinato."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿En qué fecha se fundó Lima?",
              List.of("18 de enero de 1535", "28 de julio de 1821", "9 de diciembre de 1824"),
              0,
              "El primer párrafo indica el 18 de enero de 1535."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué edificio se construyó junto a la plaza mayor?",
              List.of(
                  "El templo de Caral",
                  "La fortaleza de Sacsayhuamán",
                  "La catedral"),
              2,
              "Alrededor de la plaza mayor estaban la catedral, el palacio y el cabildo."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que Pizarro eligió un lugar cerca del mar?",
              List.of(
                  "Porque en la sierra no había ríos",
                  "Para alejarse de los barcos",
                  "Para comunicarse con España por barco"),
              2,
              "Estar cerca del mar permitía enviar y recibir barcos desde España."))),
  MICAELA_BASTIDAS(
      "micaela-bastidas",
      6,
      "Micaela Bastidas",
      List.of(
          "Micaela Bastidas fue esposa de Túpac Amaru II y una de las líderes más importantes de"
              + " la rebelión de 1780 contra los abusos de los corregidores.",
          "Ella organizaba el envío de alimentos, armas y mensajes a los rebeldes, y daba órdenes"
              + " cuando su esposo estaba en campaña.",
          "En 1781 fue capturada y ejecutada en la plaza de Armas del Cusco. Hoy es recordada"
              + " como heroína y precursora de la independencia del Perú."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿De quién trata el texto?",
              List.of(
                  "De la esposa de un virrey",
                  "De una conquistadora española",
                  "De una líder de la rebelión de 1780"),
              2,
              "El texto presenta a Micaela Bastidas como líder de la rebelión de 1780."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué hacía Micaela Bastidas durante la rebelión?",
              List.of(
                  "Organizaba alimentos, armas y mensajes",
                  "Gobernaba el virreinato",
                  "Dirigía la Expedición Libertadora"),
              0,
              "El segundo párrafo describe su trabajo de organización."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Dónde fue ejecutada?",
              List.of("En Lima", "En la plaza de Armas del Cusco", "En Cajamarca"),
              1,
              "Fue ejecutada en 1781 en la plaza de Armas del Cusco."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué se le considera precursora de la independencia?",
              List.of(
                  "Porque firmó el acta de independencia",
                  "Porque luchó contra los abusos del gobierno colonial antes de 1821",
                  "Porque llegó al Perú con San Martín"),
              1,
              "Su lucha ocurrió décadas antes de la independencia y la anticipó."))),
  LAS_ISLAS_DEL_GUANO(
      "las-islas-del-guano",
      7,
      "Las islas del guano",
      List.of(
          "Frente a la costa de Pisco están las islas Chincha. Durante siglos, millones de aves"
              + " marinas como el guanay y el piquero dejaron allí su excremento, llamado guano.",
          "Hacia 1840 los agricultores de Europa descubrieron que el guano era un excelente abono."
              + " El Perú empezó a venderlo en grandes cantidades y el Estado recibió mucho dinero.",
          "Sin embargo, el trabajo en las islas era muy duro. Gran parte del dinero se gastó en"
              + " pagar deudas y en obras públicas, y cuando el guano se agotó el país volvió a"
              + " tener problemas económicos."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿De qué trata principalmente el texto?",
              List.of(
                  "De la riqueza que trajo el guano al Perú y sus consecuencias",
                  "De la vida de los pescadores de Pisco",
                  "De la llegada de los españoles a las islas"),
              0,
              "El texto explica cómo el guano dio dinero al Perú y qué pasó después."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué aves dejaban el guano en las islas?",
              List.of("El cóndor y el colibrí", "El guanay y el piquero", "El loro y el tucán"),
              1,
              "El primer párrafo menciona al guanay y al piquero."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Para qué se usaba el guano en Europa?",
              List.of("Para construir barcos", "Para alimentar al ganado", "Como abono"),
              2,
              "El texto dice que los agricultores lo usaban como abono."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que el país volvió a tener problemas cuando se agotó el guano?",
              List.of(
                  "Porque dependía mucho del dinero de un solo producto",
                  "Porque las aves dejaron de volar",
                  "Porque Europa dejó de cultivar"),
              0,
              "Al depender casi solo del guano, la economía se debilitó cuando se acabó."))),
  EL_COMBATE_DE_ANGAMOS(
      "el-combate-de-angamos",
      8,
      "El combate de Angamos",
      List.of(
          "Al comienzo de la Guerra del Pacífico, el monitor Huáscar, comandado por Miguel Grau,"
              + " recorrió la costa enemiga y detuvo durante meses el avance chileno por mar.",
          "Grau era conocido por su trato humano: después del combate de Iquique rescató a los"
              + " marinos enemigos que estaban en el agua y escribió a la viuda del capitán"
              + " chileno Arturo Prat.",
          "El 8 de octubre de 1879, cerca de Punta Angamos, varios barcos chilenos rodearon al"
              + " Huáscar. Grau murió en el combate y desde entonces es recordado como el"
              + " Caballero de los Mares."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿Cuál es la idea principal del texto?",
              List.of(
                  "La construcción del puerto de Iquique",
                  "La campaña del Huáscar y el heroísmo de Miguel Grau",
                  "El fin de la Guerra del Pacífico"),
              1,
              "El texto narra la campaña del Huáscar y las acciones de Grau."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Cuándo fue el combate de Angamos?",
              List.of(
                  "El 28 de julio de 1821", "El 9 de diciembre de 1824", "El 8 de octubre de 1879"),
              2,
              "El tercer párrafo indica la fecha: 8 de octubre de 1879."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿A quién le escribió Grau después del combate de Iquique?",
              List.of(
                  "A la viuda de Arturo Prat",
                  "Al presidente de Bolivia",
                  "Al virrey del Perú"),
              0,
              "El segundo párrafo dice que escribió a la viuda del capitán Arturo Prat."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que lo llamaron el Caballero de los Mares?",
              List.of(
                  "Porque tenía muchos barcos",
                  "Porque fue valiente y trató con respeto incluso a sus enemigos",
                  "Porque nació en un barco"),
              1,
              "Su valentía y su trato humano con los enemigos le dieron ese nombre."))),
  LA_PATRIA_NUEVA(
      "la-patria-nueva",
      9,
      "La Patria Nueva",
      List.of(
          "En 1919 Augusto B. Leguía llegó al poder y prometió una Patria Nueva. Gobernó hasta"
              + " 1930, por eso su gobierno se conoce como el Oncenio.",
          "Con préstamos de bancos de Estados Unidos, construyó carreteras, avenidas, plazas y"
              + " obras de agua y desagüe. Para hacer las carreteras se obligó a los campesinos a"
              + " trabajar gratis mediante la ley de conscripción vial.",
          "Leguía se reeligió varias veces y persiguió a sus opositores. En 1930, en medio de una"
              + " crisis económica mundial, fue derrocado."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿De qué trata principalmente el texto?",
              List.of(
                  "Del gobierno de Leguía, sus obras y sus abusos",
                  "De la independencia del Perú",
                  "De la construcción del ferrocarril central"),
              0,
              "El texto describe el Oncenio: sus obras, cómo se hicieron y cómo terminó."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Con qué dinero se hicieron las obras de Leguía?",
              List.of(
                  "Con la venta de guano",
                  "Con impuestos a los incas",
                  "Con préstamos de bancos de Estados Unidos"),
              2,
              "El segundo párrafo menciona los préstamos de Estados Unidos."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué obligaba la ley de conscripción vial?",
              List.of(
                  "A pagar un impuesto por usar las carreteras",
                  "A trabajar gratis en la construcción de carreteras",
                  "A comprar un automóvil"),
              1,
              "Los campesinos debían trabajar sin pago en las carreteras."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que la crisis económica mundial afectó al gobierno de Leguía?",
              List.of(
                  "Porque su gobierno dependía de préstamos del extranjero",
                  "Porque el Perú no tenía carreteras",
                  "Porque Leguía vivía en Estados Unidos"),
              0,
              "Sin préstamos extranjeros no podía sostener sus obras ni su gobierno."))),
  LA_TIERRA_PARA_QUIEN_LA_TRABAJA(
      "la-tierra-para-quien-la-trabaja",
      10,
      "La tierra para quien la trabaja",
      List.of(
          "Hasta mediados del siglo XX, muchas tierras del Perú pertenecían a pocos hacendados."
              + " Los campesinos trabajaban en ellas a cambio de un pequeño pago o de una parcela.",
          "El 24 de junio de 1969, el gobierno de Juan Velasco Alvarado anunció la reforma"
              + " agraria con el lema \"Campesino, el patrón ya no comerá más de tu pobreza\".",
          "Las haciendas pasaron a cooperativas formadas por los campesinos. La reforma cambió"
              + " la propiedad de la tierra, aunque muchas cooperativas tuvieron problemas para"
              + " producir."),
      List.of(
          new PreguntaLectura(
              Tema.IDEA_PRINCIPAL,
              "¿Cuál es la idea principal del texto?",
              List.of(
                  "La creación de nuevas ciudades en la selva",
                  "La llegada de hacendados extranjeros",
                  "La reforma agraria que entregó las haciendas a los campesinos"),
              2,
              "El texto explica por qué se hizo la reforma agraria y qué cambió."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿Qué gobierno anunció la reforma agraria?",
              List.of(
                  "El de Juan Velasco Alvarado",
                  "El de Ramón Castilla",
                  "El de Augusto B. Leguía"),
              0,
              "El segundo párrafo menciona al gobierno de Juan Velasco Alvarado."),
          new PreguntaLectura(
              Tema.DATOS_DEL_TEXTO,
              "¿A quiénes pasaron las haciendas?",
              List.of(
                  "A empresas extranjeras",
                  "A cooperativas formadas por los campesinos",
                  "A los antiguos hacendados"),
              1,
              "El tercer párrafo dice que pasaron a cooperativas de campesinos."),
          new PreguntaLectura(
              Tema.INFERENCIA,
              "¿Por qué crees que se eligió el lema de la reforma?",
              List.of(
                  "Para invitar a los hacendados a comprar más tierras",
                  "Para anunciar que los campesinos dejarían de trabajar para un patrón",
                  "Para promover el turismo en el campo"),
              1,
              "El lema anunciaba el fin de la dependencia de los campesinos frente al patrón."))),
  ;

  public enum Tema {
    IDEA_PRINCIPAL("Idea principal"),
    DATOS_DEL_TEXTO("Datos del texto"),
    INFERENCIA("Inferencia");

    private final String nombre;

    Tema(String nombre) {
      this.nombre = nombre;
    }

    public String getNombre() {
      return nombre;
    }
  }

  public record PreguntaLectura(
      Tema tema,
      String enunciado,
      List<String> opciones,
      int respuestaCorrecta,
      String explicacion) {

    public boolean acepta(Integer respuesta) {
      return respuesta != null && respuesta == respuestaCorrecta;
    }

    public Optional<String> opcion(Integer indice) {
      if (indice == null || indice < 0 || indice >= opciones.size()) {
        return Optional.empty();
      }
      return Optional.of(opciones.get(indice));
    }
  }

  private final String codigo;
  private final int unidad;
  private final String titulo;
  private final List<String> parrafos;
  private final List<PreguntaLectura> preguntas;

  Lectura(
      String codigo,
      int unidad,
      String titulo,
      List<String> parrafos,
      List<PreguntaLectura> preguntas) {
    this.codigo = codigo;
    this.unidad = unidad;
    this.titulo = titulo;
    this.parrafos = parrafos;
    this.preguntas = preguntas;
  }

  public static Optional<Lectura> porCodigo(String codigo) {
    return Arrays.stream(values()).filter(l -> l.codigo.equals(codigo)).findFirst();
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

  public List<String> getParrafos() {
    return parrafos;
  }

  public List<PreguntaLectura> getPreguntas() {
    return preguntas;
  }
}
