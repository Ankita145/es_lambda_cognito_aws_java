package example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;

public class RestAssuredUtil {
  private static String user = System.getenv("puser");
  private static String pass = System.getenv("ppassword");
  private static String host = System.getenv("phost");
  private static String port = System.getenv("pport");
  private static ProxySpecification proxy =
      new ProxySpecification(host, Integer.parseInt(port), "http").withAuth(user, pass);

  public static RequestSpecification given() {
    return RestAssured.given()
        .proxy(proxy)
        .relaxedHTTPSValidation()
        .log()
        .all()
        .contentType(ContentType.JSON);
  }
}
