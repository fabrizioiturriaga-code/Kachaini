package kanichi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import kanichi.model.Liga;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** HU-14: ligas por XP semanal, de Amauta (más baja) a Inca (más alta). */
class LigaTest {
  @ParameterizedTest
  @CsvSource({
    "0, AMAUTA",
    "39, AMAUTA",
    "40, PANACA",
    "79, PANACA",
    "80, AUQUI",
    "119, AUQUI",
    "120, INCA",
    "500, INCA"
  })
  void laLigaDependeDeLosUmbralesDeXpSemanal(int xp, Liga esperada) {
    assertEquals(esperada, Liga.para(xp));
  }

  @Test
  void seSubeDeUnaLigaALaVezEnOrdenJerarquico() {
    assertEquals(Optional.of(Liga.PANACA), Liga.AMAUTA.siguiente());
    assertEquals(Optional.of(Liga.AUQUI), Liga.PANACA.siguiente());
    assertEquals(Optional.of(Liga.INCA), Liga.AUQUI.siguiente());
    assertEquals(Optional.empty(), Liga.INCA.siguiente());
  }

  @Test
  void losUmbralesSonCrecientes() {
    Liga[] ligas = Liga.values();
    for (int i = 1; i < ligas.length; i++) {
      assertTrue(ligas[i].getXpMinima() > ligas[i - 1].getXpMinima());
    }
  }
}