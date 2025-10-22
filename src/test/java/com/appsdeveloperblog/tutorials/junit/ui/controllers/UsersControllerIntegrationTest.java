package com.appsdeveloperblog.tutorials.junit.ui.controllers;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersControllerIntegrationTest {


  @Value("${server.port}")
  private int serverPort;


  @LocalServerPort
  private int localServerPort;


  private String authorizationToken;
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();


  private String registerAndLogin(String email, String password) throws JSONException {
    JSONObject signupPayload = new JSONObject()
        .put("firstName", "Eden")
        .put("lastName", "Bercier")
        .put("email", email)
        .put("password", password)
        .put("repeatPassword", password);

    given()
        .contentType(ContentType.JSON)
        .body(signupPayload.toString())
        .when()
        .post("/users")
        .then()
        .statusCode(anyOf(is(200), is(201), is(204)));

    JSONObject loginPayload = new JSONObject()
        .put("email", email)
        .put("password", password);

    Response response = given()
        .contentType(ContentType.JSON)
        .body(loginPayload.toString())
        .when()
        .post("/login");

    assertEquals(200, response.getStatusCode(), "Login failed for " + email);
    return response.getHeader(SecurityConstants.HEADER_STRING).replace("Bearer ", "");
  }

  @Test
  @DisplayName("User can be created")
  @Order(1)
  void testCreateUser_whenValidDetailsProvided_returnsUserDetails() throws JSONException {
    String email = "Create_" + UUID.randomUUID() + "@test.com";

    JSONObject userDetailsRequestJson = new JSONObject();
    userDetailsRequestJson.put("firstName", "Eden");
    userDetailsRequestJson.put("lastName", "Bercier");
    userDetailsRequestJson.put("email", email);
    userDetailsRequestJson.put("password", "12345678");
    userDetailsRequestJson.put("repeatPassword", "12345678");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<String> request = new HttpEntity<>(userDetailsRequestJson.toString(), headers);

    ResponseEntity<UserRest> createdUserDetailsEntity = testRestTemplate.postForEntity("/users",
        request, UserRest.class);
    UserRest createdUserDetails = createdUserDetailsEntity.getBody();

    assertEquals(HttpStatus.OK, createdUserDetailsEntity.getStatusCode());
    assertEquals(userDetailsRequestJson.getString("firstName"), createdUserDetails.getFirstName());
    assertEquals(userDetailsRequestJson.getString("lastName"), createdUserDetails.getLastName());
    assertEquals(userDetailsRequestJson.getString("email"), createdUserDetails.getEmail());
    assertFalse(createdUserDetails.getUserId().trim().isEmpty());
  }

  @Test
  @DisplayName("GET /users requires JWT")
  @Order(2)
  void testGetUsers_whenMissingJWT_returns403() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

    ResponseEntity<List<UserRest>> response = testRestTemplate.exchange(
        "/users",
        HttpMethod.GET,
        requestEntity,
        new ParameterizedTypeReference<>() {
        }
    );

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }


  @Test
  @DisplayName("/login works")
  @Order(3)
  void testUserLogin_whenValidCredentialsProvided_ReturnsJWTAuthorizationHeader()
      throws JSONException {
    JSONObject loginCredentials = new JSONObject();
    loginCredentials.put("email", "edenbercier@gmail.com");
    loginCredentials.put("password", "12345678");

    HttpEntity<String> request = new HttpEntity<>(loginCredentials.toString());

    ResponseEntity<Void> response = testRestTemplate.postForEntity("/users/login", request,
        Void.class);

    authorizationToken = response.getHeaders().getFirst(SecurityConstants.HEADER_STRING);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(authorizationToken);
    assertNotNull(response.getHeaders().getFirst("UserID"));
  }


  @Test
  @Order(4)
  @DisplayName("GET /users works")
  void testGetUsers_whenValidJWTProvided_returnsUsers() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setBearerAuth(authorizationToken);

    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    ResponseEntity<List<UserRest>> response = testRestTemplate.exchange(
        "/users",
        HttpMethod.GET,
        requestEntity,
        new ParameterizedTypeReference<>() {
        }
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }
}