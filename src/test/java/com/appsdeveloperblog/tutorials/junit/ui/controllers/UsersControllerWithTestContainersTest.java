package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import com.appsdeveloperblog.userservice.ui.model.User;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UsersControllerWithTestContainersTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UsersService usersService;

    private String authorizationToken;

    @Container
    @ServiceConnection
    private static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.4.0");

    static {
        mySQLContainer.start();
    }

    @LocalServerPort
    private int port;

    @BeforeAll
    void setupRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @BeforeAll
    void seedTestUser() {
        System.out.println("Seeding initial test user...");

        // Directly create UserDto (no need for UserDetailsRequestModel)
        UserDto dto = new UserDto();

        dto.setFirstName("Eden");
        dto.setLastName("Bee");
        dto.setEmail("edenbercier@gmail.com");
        dto.setPassword("12345678");

        try {
            usersService.createUser(dto);
            System.out.println("✅ Seed user created before tests.");
        } catch (Exception e) {
            System.out.println("ℹ️ Seed user may already exist: " + e.getMessage());
        }
    }


    @BeforeEach
    void loginBeforeEach() throws JSONException {
        JSONObject loginCredentials = new JSONObject();
        loginCredentials.put("email", "edenbercier@gmail.com");
        loginCredentials.put("password", "12345678");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(loginCredentials.toString(), headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity("/login", request, Void.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Login should succeed");

        authorizationToken = response.getHeaders()
                .getValuesAsList(SecurityConstants.HEADER_STRING)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Authorization header after login"));

        assertNotNull(authorizationToken, "Authorization token must not be null");
    }

    void clearUsersTable() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    @Order(1)
    @DisplayName("The MySQL container is created and running")
    void isTestContainerRunning() {
        assertTrue(mySQLContainer.isCreated(), "MySQL container has not been created");
        assertTrue(mySQLContainer.isRunning(), "MySQL container is not running");
    }

    @Test
    @Order(2)
    @DisplayName("GET /Users requires JWT")
    void testGetUsers_whenMissingJWT_returns403() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<?> requestEntity = new HttpEntity<>(null, headers);

        // Act
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {});

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Http status code should have been 403");
    }

    @Test
    @Order(3)
    @DisplayName("GET /users works with valid JWT")
    void testGetUsers_whenValidJWTProvided_returnsUsers() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authorizationToken.replace("Bearer ",""));

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {});

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "HTTP Status code should be 200");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().size() >= 1,
                "There should be at least one user in the list");
    }
}