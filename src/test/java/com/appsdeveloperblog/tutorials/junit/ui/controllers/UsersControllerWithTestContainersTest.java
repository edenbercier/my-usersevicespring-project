package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
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
    RestAssured.filters(new RequestLoggingFilter());
    RestAssured.filters(new ResponseLoggingFilter());

    RestAssured.requestSpecification = new RequestSpecBuilder()
        .setContentType(ContentType.JSON)
        .setAccept(ContentType.JSON)
        .addFilter(new RequestLoggingFilter())
        .build();
    RestAssured.responseSpecification = new ResponseSpecBuilder()
        //    .expectStatusCode(anyOf(is(200),is(201)))
        .expectResponseTime(lessThan(2000L))
        //  .expectBody("id", notNullValue())
        .build();

    // 1️⃣ Register the user via HTTP
    JSONObject signupPayload = new JSONObject()
        .put("firstName", "Eden")
        .put("lastName", "Bercier")
        .put("email", "test@test.com")
        .put("password", "123456789")
        .put("repeatPassword", "123456789");

    given()
        .contentType(ContentType.JSON)
        .body(signupPayload.toString())
        .when()
        .post("/users")
        .then()
        .statusCode(anyOf(is(200), is(201)));

    // 2️⃣ Retry login (polling up to 5 times if needed)
    loginPayload = new JSONObject()
        .put("email", "test@test.com")
        .put("password", "123456789");

    int retries = 5;
    for (int i = 0; i < retries; i++) {
      Response response = given()
          .contentType(ContentType.JSON)
          .body(loginPayload.toString())
          .when()
          .post("/login");

      if (response.statusCode() == 200) {
        this.authorizationToken = response
            .getHeader(SecurityConstants.HEADER_STRING)
            .replace("Bearer ", "");
        return;
      }

      try {
        Thread.sleep(200); // short, controlled delay between retries
      } catch (InterruptedException ignored) {
      }
    }

    throw new IllegalStateException("Failed to log in after registering user.");
  }

  @Test
  @DisplayName("The MySQL container is created and running")
  void isTestContainerRunning() {

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
  @DisplayName("GET /users Login works with valid JWT")
  void testUserLogin_withValidCredentials_shouldTokenAndUserId() {
    // Arrange
    Map<String, Object> loginPayload = Map.of(
        "email", TEST_EMAIL,
        "password", TEST_PASSWORD
    );
    Matcher<?> idNotNull = notNullValue();
    // Act
    given()
        .contentType(ContentType.JSON)
        .body(loginPayload)
        .when()
        .post("/login")
        .then()
        .statusCode(200)
        .header(SecurityConstants.HEADER_STRING, not(emptyOrNullString()))
        .body("id", idNotNull);

  }

}