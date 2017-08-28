package example;

public interface Constants {
  String HOST_URL = System.getenv("ES_HOST");
  String PORT = System.getenv("ES_PORT");
  String PROTOCOL = System.getenv("ES_PROTOCOL");
}
