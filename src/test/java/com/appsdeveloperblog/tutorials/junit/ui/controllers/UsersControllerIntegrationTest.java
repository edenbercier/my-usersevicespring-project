package com.appsdeveloperblog.tutorials.junit.ui.controllers;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.appsdeveloperblog.tutorials.junit.io.UsersRepository;
import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersControllerIntegrationTest {


  @Value("${server.port}")
  private int serverPort;
  private final String TEST_EMAIL = "login_" + UUID.randomUUID() + "@test.com";
  private final String TEST_PASSWORD = "12345678";

  private String authorizationToken;
  @LocalServerPort
  private int localServerPort;
  @Autowired
  private UsersService usersService;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private UsersRepository usersRepository;


  @BeforeEach
  void cleanDatabase() {
    usersRepository.deleteAll();
  }

  @Test
  @DisplayName("User can be created")
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
  @DisplayName("/login works when valid user exists")
  void testUserLogin_whenValidCredentialsProvided_ReturnsJWTAuthorizationHeader() throws JSONException {
    // Step 1: Create user first
    UserDto user = new UserDto();
    user.setEmail(TEST_EMAIL);
    user.setPassword(TEST_PASSWORD);
    user.setFirstName("Test");
    user.setLastName("User");
    usersService.createUser(user);

    // Step 2: Login with correct credentials
    JSONObject loginCredentials = new JSONObject();
    loginCredentials.put("email", TEST_EMAIL);
    loginCredentials.put("password", TEST_PASSWORD);

    HttpEntity<String> request = new HttpEntity<>(loginCredentials.toString());
    ResponseEntity<String> response = testRestTemplate.postForEntity("/login", request, String.class);

    // Step 3: Parse token and userId from JSON body
    JSONObject body = new JSONObject(response.getBody());
    String token = body.getString("token");
    String userId = body.getString("userId");

    // Step 4: Validate response
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(token);
    assertNotNull(userId);
  }


  @Test
  @DisplayName("GET /users works with valid JWT")
  void testGetUsers_withValidJwt_returnsUsers() throws JSONException {
    // Create user
    UserDto user = new UserDto();
    user.setEmail(TEST_EMAIL);
    user.setPassword(TEST_PASSWORD);
    user.setFirstName("Test");
    user.setLastName("User");
    usersService.createUser(user);

    // Login and extract token
    JSONObject login = new JSONObject()
        .put("email", TEST_EMAIL)
        .put("password", TEST_PASSWORD);

    HttpEntity<String> loginRequest = new HttpEntity<>(login.toString());
    ResponseEntity<String> loginResponse = testRestTemplate.postForEntity("/login", loginRequest, String.class);
    String token = new JSONObject(loginResponse.getBody()).getString("token");

    // GET /users with token
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<List<UserRest>> response = testRestTemplate.exchange(
        "/users",
        HttpMethod.GET,
        request,
        new ParameterizedTypeReference<>() {}
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }
}