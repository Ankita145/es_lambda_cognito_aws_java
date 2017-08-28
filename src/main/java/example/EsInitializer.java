package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class EsInitializer {

  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Usage: program host_url port protocol path_to_layout path_to_csv");
      System.exit(0);
    }

    try {
      new EsInitializer(args[0], args[1], args[2], args[3], args[4]).run();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private final ObjectMapper mapper = new ObjectMapper();
  private final String layoutPath;
  private final String csvPath;
  private RestClient restClient;

  public EsInitializer(
      String hostUrl, String port, String protocol, String layoutPath, String csvPath) {
    this.layoutPath = layoutPath;
    this.csvPath = csvPath;
    restClient =
        RestClient.builder(new HttpHost(hostUrl, Integer.parseInt(port), protocol)).build();
  }

  public void run() throws IOException {
    try {
      List<String> layout = readLayout(layoutPath);

      String firstLine = Files.lines(Paths.get(csvPath)).findFirst().get();
      if (!verifyLayout(layout, firstLine.split(",", layout.size()))) {
        System.out.print("The layout does not match the csv!");
        System.exit(1);
      }

      long[] idx = {0L};
      Files.lines(Paths.get(csvPath))
          .forEach(
              line -> {
                idx[0]++;
                String[] columns = line.split(",", layout.size());
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < columns.length; i++) {
                  map.put(layout.get(i), columns[i].replaceAll("\"", ""));
                }
                try {
                  Response res = indexDocument(map, idx[0]);
                  System.out.println(
                      String.format(
                          "Inserting doc[%d], status=%d",
                          idx[0], res.getStatusLine().getStatusCode()));
                } catch (Exception e) {
                  e.printStackTrace();
                }
              });
    } finally {
      restClient.close();
    }
  }

  private Response indexDocument(Map<String, String> doc, long id) throws Exception {
    HttpEntity entity = new NStringEntity(mapper.writeValueAsString(doc));
    return restClient.performRequest("PUT", "/example/plan/" + id, Collections.emptyMap(), entity);
  }

  private List<String> readLayout(String path) throws IOException {
    return Files.lines(Paths.get(path))
        .skip(2)
        .map(line -> line.split(",")[1])
        .collect(Collectors.toList());
  }

  private boolean verifyLayout(List<String> layout, String[] line) {
    for (int i = 0; i < layout.size(); i++) {
      if (!layout.get(i).equals(line[i])) {
        return false;
      }
    }
    return true;
  }
}
