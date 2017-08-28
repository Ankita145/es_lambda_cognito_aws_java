package example.handler;

import static example.Constants.HOST_URL;
import static example.Constants.PORT;
import static example.Constants.PROTOCOL;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.model.SearchPlanRequest;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

/**
 * Search Lambda function.
 */
public class SearchPlanHandler implements RequestHandler<SearchPlanRequest, String> {
  static final Logger log = Logger.getLogger(SearchPlanHandler.class);

  private final ObjectMapper mapper = new ObjectMapper();
  private RestClient restClient =
      RestClient.builder(new HttpHost(HOST_URL, Integer.parseInt(PORT), PROTOCOL)).build();

  public String handleRequest(SearchPlanRequest input, Context context) {
    long start = System.currentTimeMillis();
    try {
      log.debug("INPUT=" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
      String queryStr = constructESQuery(input);
      return search(queryStr);
    } catch (Exception e) {
      log.error(e.getMessage());
      return "Error: " + e.getMessage();
    } finally {
      log.info(String.format("CallTime: %dms", System.currentTimeMillis() - start));
    }
  }

  private String constructESQuery(Object input) throws JsonProcessingException {
    Map<String, Object> map = new HashMap<>();
    map.put("query", input);
    return mapper.writeValueAsString(map);
  }

  private String search(String queryStr) throws Exception {
    log.info("QueryString: " + queryStr);
    HttpEntity entity = new NStringEntity(queryStr, ContentType.APPLICATION_JSON);
    Response response =
        restClient.performRequest("GET", "/example/plan/_search", new HashMap(), entity);
    String result = EntityUtils.toString(response.getEntity());
    log.info("OUTPUT: " + result);
    return result;
  }
}
