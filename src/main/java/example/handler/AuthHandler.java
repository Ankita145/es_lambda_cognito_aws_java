package example.handler;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.model.AuthRequest;
import example.model.AuthResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Authentication Lambda function.
 */
public class AuthHandler implements RequestHandler<AuthRequest, AuthResponse> {

  static final Logger log = Logger.getLogger(AuthHandler.class);

  private final ObjectMapper mapper = new ObjectMapper();

  public AuthResponse handleRequest(AuthRequest input, Context context) {
    long start = System.currentTimeMillis();
    try {
      context.getLogger().log("Input: " + mapper.writeValueAsString(input));
      String appClientId = System.getenv("APP_CLIENT_ID");
      String userPoolId = System.getenv("USER_POOL_ID");
      log.debug("Input=" + input);
      log.debug("USER_POOL_ID=" + userPoolId);
      log.debug("APP_CLIENT_ID=" + appClientId);
      AWSCognitoIdentityProvider client = AWSCognitoIdentityProviderClientBuilder.defaultClient();
      AdminInitiateAuthRequest request = new AdminInitiateAuthRequest();
      request.setClientId(appClientId);
      request.setUserPoolId(userPoolId);
      request.setAuthFlow("ADMIN_NO_SRP_AUTH");
      Map<String, String> params = new HashMap<>();
      params.put("USERNAME", input.getUser());
      params.put("PASSWORD", input.getPass());
      request.setAuthParameters(params);
      AdminInitiateAuthResult result = client.adminInitiateAuth(request);
      log.debug("authResult: " + mapper.writeValueAsString(result));
      AuthenticationResultType authResult = result.getAuthenticationResult();
      AuthResponse ar = new AuthResponse();
      ar.setIdToken(authResult.getIdToken());
      ar.setRefreshToken(authResult.getRefreshToken());
      ar.setAccessToken(authResult.getAccessToken());
      ar.setExpiresIn(authResult.getExpiresIn());
      ar.setTokenType(authResult.getTokenType());
      return ar;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      log.info(String.format("CallTime: %dms", System.currentTimeMillis() - start));
    }
    return new AuthResponse();
  }
}
