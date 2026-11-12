package kanichi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public enum ActividadArrastre {
  LUGARES_DEL_POBLAMIENTO(
      "lugares-del-poblamiento",
      1,
      "Lugares de los primeros pobladores",
      "Arrastra cada lugar hasta lo que se encontró allí.",
      List.of(
          new Pareja("Lauricocha", "Restos humanos de antiguos cazadores en Huánuco"),
          new Pareja("Toquepala", "Pinturas rupestres de caza en Tacna"),
          new Pareja("Pikimachay", "Cueva de antiguos cazadores en Ayacucho"),
          new Pareja("Guitarrero", "Cueva en Áncash con restos de plantas cultivadas"))),
  LEGADO_PREINCA(
      "legado-preinca",
      2,
      "Culturas preíncas y su legado",
      "Arrastra cada cultura hasta su aporte más conocido.",
      List.of(
          new Pareja("Caral", "La ciudad más antigua de América"),
          new Pareja("Chavín", "El Lanzón, escultura de su dios principal"),
          new Pareja("Paracas", "Mantos funerarios bordados"),
          new Pareja("Chimú", "Chan Chan, la gran ciudad de adobe"))),
  ORGANIZACION_INCA(
      "organizacion-inca",
      3,
      "La organización del Tahuantinsuyo",
      "Arrastra cada palabra hasta su significado.",
      List.of(
          new Pareja("Ayni", "Ayuda mutua entre familias"),
          new Pareja("Minka", "Trabajo comunitario en beneficio de todos"),
          new Pareja("Mita", "Trabajo por turnos para el Estado"),
          new Pareja("Quipu", "Registro hecho con cuerdas y nudos"))),
  PERSONAJES_DE_LA_CONQUISTA(
      "personajes-de-la-conquista",
      4,
      "Personajes de la conquista",
      "Arrastra cada personaje hasta lo que hizo.",
      List.of(
          new Pareja("Francisco Pizarro", "Dirigió la expedición española de 1532"),
          new Pareja("Atahualpa", "Inca capturado en Cajamarca"),
          new Pareja("Huáscar", "Hermano que disputó el poder a Atahualpa"),
          new Pareja("Manco Inca", "Lideró la resistencia desde Vilcabamba"))),
  VIDA_EN_EL_VIRREINATO(
      "vida-en-el-virreinato",
      5,
      "La vida en el virreinato",
      "Arrastra cada término hasta su descripción.",
      List.of(
          new Pareja("Virrey", "Gobernaba en nombre del rey de España"),
          new Pareja("Criollos", "Hijos de españoles nacidos en América"),
          new Pareja("Potosí", "Mina de plata más famosa del virreinato"),
          new Pareja("Huancavelica", "Mina de mercurio"))),
  FECHAS_DE_LA_INDEPENDENCIA(
      "fechas-de-la-independencia",
      6,
      "Fechas de la independencia",
      "Arrastra cada fecha hasta el hecho que ocurrió.",
      List.of(
          new Pareja("1780", "Rebelión de Túpac Amaru II"),
          new Pareja("1820", "Llegada de la Expedición Libertadora"),
          new Pareja("28 de julio de 1821", "Proclamación de la independencia"),
          new Pareja("9 de diciembre de 1824", "Batalla de Ayacucho"))),
  REPUBLICA_Y_GUANO(
      "republica-y-guano",
      7,
      "La República y la era del guano",
      "Arrastra cada elemento hasta su descripción.",
      List.of(
          new Pareja("Caudillos", "Jefes militares que se disputaban el poder"),
          new Pareja("Guano", "Abono de aves marinas vendido a Europa"),
          new Pareja("Islas Chincha", "Principal lugar de donde se extraía el guano"),
          new Pareja("Ramón Castilla", "Abolió la esclavitud en 1854"))),
  GUERRA_DEL_PACIFICO(
      "guerra-del-pacifico",
      8,
      "La Guerra del Pacífico",
      "Arrastra cada personaje o hecho hasta lo que le corresponde.",
      List.of(
          new Pareja("Miguel Grau", "Combate de Angamos"),
          new Pareja("Francisco Bolognesi", "Defensa del morro de Arica"),
          new Pareja("Andrés A. Cáceres", "Resistencia en la sierra durante la guerra"),
          new Pareja("Tratado de Ancón", "Fin de la guerra en 1883"))),
  PERU_SIGLO_XX(
      "peru-siglo-xx",
      9,
      "El Perú en el siglo XX",
      "Arrastra cada elemento hasta el hecho que le corresponde.",
      List.of(
          new Pareja("Partido Civil", "Gobernó durante la República Aristocrática"),
          new Pareja("Augusto B. Leguía", "Gobierno de la Patria Nueva"),
          new Pareja("1929", "Tacna vuelve a ser parte del Perú"),
          new Pareja("1955", "Se reconoce el voto de la mujer"))),
  PERU_CONTEMPORANEO(
      "peru-contemporaneo",
      10,
      "El Perú contemporáneo",
      "Arrastra cada elemento hasta el hecho que le corresponde.",
      List.of(
          new Pareja("Juan Velasco Alvarado", "Reforma agraria de 1969"),
          new Pareja("Constitución de 1979", "Voto de las personas analfabetas"),
          new Pareja("Comisión de la Verdad", "Informe final sobre la violencia en 2003"),
          new Pareja("Valentín Paniagua", "Gobierno de transición en 2000"))),
  ;

  public record Pareja(String elemento, String destino) {}

  public record Elemento(int indice, String texto) {}

  private final String codigo;
  private final int unidad;
  private final String titulo;
  private final String instruccion;
  private final List<Pareja> parejas;

  ActividadArrastre(
      String codigo, int unidad, String titulo, String instruccion, List<Pareja> parejas) {
    this.codigo = codigo;
    this.unidad = unidad;
    this.titulo = titulo;
    this.instruccion = instruccion;
    this.parejas = parejas;
  }

  public static Optional<ActividadArrastre> porCodigo(String codigo) {
    return Arrays.stream(values()).filter(a -> a.codigo.equals(codigo)).findFirst();
  }

  public List<Elemento> elementosMezclados() {
    List<Elemento> elementos =
        new ArrayList<>(
            IntStream.range(0, parejas.size())
                .mapToObj(i -> new Elemento(i, parejas.get(i).elemento()))
                .toList());
    Random azar = new Random(codigo.hashCode());
    do {
      Collections.shuffle(elementos, azar);
    } while (estaEnOrden(elementos));
    return List.copyOf(elementos);
  }

  private static boolean estaEnOrden(List<Elemento> elementos) {
    return IntStream.range(0, elementos.size()).allMatch(i -> elementos.get(i).indice() == i);
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

  public String getInstruccion() {
    return instruccion;
  }

  public List<Pareja> getParejas() {
    return parejas;
  }
}
