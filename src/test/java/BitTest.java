import com.diozero.util.RangeUtil;
import org.junit.jupiter.api.Test;

public class BitTest {

  @Test
  public void t00() {
    txx((short) 0x8000);
    txx((short) 0x8001);
    txx((short) 0xffff);
    txx((short) 0x0000);
    txx((short) 0x0001);
    txx((short) 0x7fff);
  }

  void txx(short conversionData) {
    float c = conversionData;
    float rv = RangeUtil.map(c, 0, 0x8000, 0, 1f, false);
    System.out.println (conversionData + " -> " + c + " -> " + rv);
  }
}
