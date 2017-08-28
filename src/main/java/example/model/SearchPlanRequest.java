package example.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchPlanRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private static class MultiMatchBody {

    private String query;
    private List<String> fields = new ArrayList<>();

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public List<String> getFields() {
      return fields;
    }

    public void setFields(List<String> fields) {
      this.fields = fields;
    }
  }

  public static class Builder {

    private SearchPlanRequest instance;

    private Builder() {
      instance = new SearchPlanRequest();
      instance.setMulti_match(new MultiMatchBody());
    };

    public Builder query(String queryStr) {
      instance.getMulti_match().setQuery(queryStr);
      return this;
    }

    public Builder field(String field) {
      instance.getMulti_match().getFields().add(field);
      return this;
    }

    public SearchPlanRequest build() {
      return instance;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  // Annotation is not respected by API Gateway
  //  @JsonProperty("multi_match")
  private MultiMatchBody multi_match;

  public MultiMatchBody getMulti_match() {
    return multi_match;
  }

  public void setMulti_match(MultiMatchBody multi_match) {
    this.multi_match = multi_match;
  }
}
