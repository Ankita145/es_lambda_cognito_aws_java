package example;

import static example.Constants.HOST_URL;
import static example.Constants.PORT;
import static example.Constants.PROTOCOL;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

public class DataTest {

  @Test
  @Ignore
  public void testLoadData() throws IOException {
    String basePath = System.getProperty("user.dir");
    String layoutPath = String.format("%s/data/layout.txt", basePath);
    String csvPath = String.format("%s/data/sample.csv", basePath);

    new EsInitializer(HOST_URL, PORT, PROTOCOL, layoutPath, csvPath).run();
    assertTrue(true);
  }
}
