package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.hamcrest.Matcher; // ✅ add this line
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersControllerWithTestContainersTest {

  @Container
  @ServiceConnection
  private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.4.0");

  private final String TEST_EMAIL = "test@test.com";
  private final String TEST_PASSWORD = "123456789";

  private JSONObject loginPayload;

  static {
    mySQLContainer.start();
  }

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private UsersService usersService;
  private String authorizationToken;
  private JSONObject signUpPayload;
  @LocalServerPort
  private int port;

  // private final RequestLoggingFilter requestLoggingFilter = new RequestLoggingFilter();
  //  private final ResponseLoggingFilter responseLoggingFilter = new ResponseLoggingFilter();
  @BeforeAll
  void setupRestAssuredAndAuthenticate() throws JSONException {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

    RestAssured.requestSpecification = new RequestSpecBuilder()
        .setContentType(ContentType.JSON)
        .setAccept(ContentType.JSON)
        .build();

    RestAssured.responseSpecification = new ResponseSpecBuilder()
        .expectResponseTime(lessThan(2000L))
        .build();

    // ✅ Create admin user directly using the service
    UserDto user = new UserDto();
    user.setFirstName("Eden");
    user.setLastName("Bercier");
    user.setEmail(TEST_EMAIL);
    user.setPassword(TEST_PASSWORD);
    user.setRoles(List.of("ADMIN")); // ✅ Set role to ADMIN
    usersService.createUser(user);

    // 2️⃣ Login to get the JWT token
    loginPayload = new JSONObject()
        .put("email", TEST_EMAIL)

        .put("password", TEST_PASSWORD);

    int retries = 5;
    for (int i = 0; i < retries; i++) {
      Response response = given()
          .contentType(ContentType.JSON)
          .body(loginPayload.toString())
          .when()
          .post("/login");

      if (response.statusCode() == 200) {
        String token = response.jsonPath().getString("token");
        if (token != null && token.startsWith("Bearer ")) {
          this.authorizationToken = token.replace("Bearer ", "");
          return;
        } else {
          throw new IllegalStateException("Token missing or malformed in body: " + response.asString());
        }
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException ignored) {}
    }

    throw new IllegalStateException("Failed to log in after registering user.");
  }
  private String extractRoleFromJwt(String jwt) {
    String secret = SecurityConstants.TOKEN_SECRET; // <- use constant
    Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();
    return claims.get("role", String.class);
  }

  @Test
  @DisplayName("The MySQL container is created and running")
  void isTestContainerRunning() {

    assertEquals("ADMIN", extractRoleFromJwt(authorizationToken), "Expected ADMIN role in JWT");

    given()

        .auth()
        .oauth2(authorizationToken)
        .accept(ContentType.JSON)

        .when()
        .get("/users")

        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("[0].email", not(emptyOrNullString()));
  }
  @Test
  @DisplayName("POST /register fails when passwords do not match")
  void testRegisterUser_whenPasswordsDoNotMatch_returnsError() throws JSONException {
    JSONObject payload = new JSONObject()
        .put("firstName", "Mismatch")
        .put("lastName", "Case")
        .put("email", "fail@test.com")
        .put("password", "password1")
        .put("repeatPassword", "password2");

    given()
        .contentType(ContentType.JSON)
        .body(payload.toString())
        .when()
        .post("/register")
        .then()
        .statusCode(500) // or 400 depending on how you handle mismatch
        .body("message", not(emptyOrNullString()));
  }



  @Test
  @DisplayName("GET /Users access fails when Missing JWT")
  void testGetUsers_whenMissingJWT_returns403() {
    // Arrange
    given()
        .accept(ContentType.JSON)

        .when()
        .get("/users")
        .then()
        .statusCode(403);
  }

  @Test
  @DisplayName("GET /users authorized access works with valid JWT succeds")
  void testAuthorizedAccessToUsers_withValidJWTToken_shouldReturnUsers() {
    assertEquals("ADMIN", extractRoleFromJwt(authorizationToken), "Expected ADMIN role in JWT");

    given()
        .auth()
        .oauth2(authorizationToken)
        .accept(ContentType.JSON)

        .when()
        .get("/users")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("[0].email", not(emptyOrNullString()));
  }

  @Test
  @DisplayName("POST /login returns token and userId in JSON body")
  void testUserLogin_withValidCredentials_returnsJwtAndUserId() {
    Map<String, String> loginPayload = Map.of(
        "email", TEST_EMAIL,
        "password", TEST_PASSWORD
    );

    given()
        .contentType(ContentType.JSON)
        .body(loginPayload)
        .when()
        .post("/login")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("userId", not(emptyOrNullString()))
        .body("token", not(emptyOrNullString()));
  }

  @Test
  @DisplayName("GET /users returns User Info when JWT is valid")
  void testGetUser_withValidJWT_returnsUserDetails() {
    // Ensure our token is ADMIN
    assertEquals("ADMIN", extractRoleFromJwt(authorizationToken), "Expected ADMIN role in JWT");

    given()
        .auth()
        .oauth2(authorizationToken)
        .accept(ContentType.JSON)
        .when()
        .get("/users")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        // Find the user by email instead of assuming index 0
        .body(
            String.format("find { it.email == '%s' }.email", TEST_EMAIL),
            equalTo(TEST_EMAIL)
        )
        .body(
            String.format("find { it.email == '%s' }.firstName", TEST_EMAIL),
            equalTo("Eden")
        )
        .body(
            String.format("find { it.email == '%s' }.lastName", TEST_EMAIL),
            equalTo("Bercier")
        )
        .body(
            String.format("find { it.email == '%s' }.userId", TEST_EMAIL),
            not(emptyOrNullString())
        )
        .body(String.format("find { it.email == '%s' }.role", TEST_EMAIL), equalTo("ADMIN")
        );


  }


}
