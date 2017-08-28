package example;

import static example.RestAssuredUtil.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.model.AuthRequest;
import example.model.AuthResponse;
import example.model.SearchPlanRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;

public class EndpointTest {
  protected ObjectMapper mapper = new ObjectMapper();

  @Before
  public void setup() {
    RestAssured.baseURI = "https://ew29pp2ecj.execute-api.us-east-1.amazonaws.com/dev";
  }

  @Test
  public void search() throws Exception {
    AuthResponse authResult = callAuth();

    SearchPlanRequest query =
        SearchPlanRequest.builder()
            .query("MILLER")
            .field("PLAN_NAME")
            .field("SPONSOR_DFE_NAME")
            .field("SPONS_DFE_LOC_US_STATE")
            .build();

    String json = mapper.writeValueAsString(query);

    SearchPlanRequest req = mapper.readValue(json, SearchPlanRequest.class);
    System.out.println("JSON>>>" + json);
    ValidatableResponse result =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", authResult.getIdToken())
            .body(json)
            .when()
            .post("/search")
            .then()
            .statusCode(200);
    String body = result.extract().body().asString();
    String str = mapper.readValue(body, String.class);
    assertTrue(str.contains("MILLER"));
    Object obj = mapper.readValue(str, Object.class);
    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
  }

  @Test
  public void auth() throws Exception {
    AuthResponse authResult = callAuth();
    assertNotNull(authResult);
    assertNotNull(authResult.getIdToken());
  }

  private AuthResponse callAuth() throws Exception {
    AuthRequest request = new AuthRequest();
    request.setUser("alex");
    request.setPass("Pa$$w0rd");
    ValidatableResponse authResponse =
        given()
            .contentType(ContentType.JSON)
            .body(mapper.writeValueAsString(request))
            .when()
            .post("/auth")
            .then()
            .statusCode(200);
    String body = authResponse.extract().body().asString();
    System.out.println(body);
    AuthResponse authResult = mapper.readValue(body, AuthResponse.class);
    return authResult;
  }
}
