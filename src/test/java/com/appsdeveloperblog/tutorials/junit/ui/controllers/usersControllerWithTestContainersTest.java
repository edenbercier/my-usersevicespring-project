package com.appsdeveloperblog.tutorials.junit.ui.controllers;
import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersControllerWithTestContainersTest {
@Autowired
private TestRestTemplate testRestTemplate;
    private String authorizationToken;

    @ServiceConnection
    private static  MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.4.0");

    static {
        mySQLContainer.start();
    }

//            .withDatabaseName("photo_app")
//            .withUsername("test")
//            .withPassword("test");
//
//    // Added back: explicitly map container props into Spring's datasource
//    @DynamicPropertySource
//    static void overrideProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", mySQL::getJdbcUrl);
//        registry.add("spring.datasource.username", mySQL::getUsername);
//        registry.add("spring.datasource.password", mySQL::getPassword);
//        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
//    }

    @Order(1)
    @Test
    @DisplayName("The MySQL container is created and running")
    void isTestContainerRunning() {
        assertTrue(mySQLContainer.isCreated(), "MySQL container has not been created");
        assertTrue(mySQLContainer.isRunning(), "MySQL container is not running");
    }
    @Order(2)
    @Test
    @DisplayName("User can be created")
    //@Order(1)
    void testCreateUser_whenValidDetailsProvided_returnsUserDetails() throws JSONException {
        // Arrange
        //Create Json Object turn into script

        JSONObject userDetailsRequestJson = new JSONObject();
        userDetailsRequestJson.put("firstName", "Eden");
        userDetailsRequestJson.put("lastName", "Bercier");
        userDetailsRequestJson.put("email", "edenbercier@gmail.com");
        userDetailsRequestJson.put("password", "12345678");
        userDetailsRequestJson.put("repeatPassword", "12345678");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> request = new HttpEntity<>(userDetailsRequestJson.toString(), headers);

        // Act
        ResponseEntity<UserRest> createdUserDetailsEntity = testRestTemplate.postForEntity("/users",
                request,
                UserRest.class);
        UserRest createdUserDetails = createdUserDetailsEntity.getBody();

        // Assert
        Assertions.assertEquals(HttpStatus.OK, createdUserDetailsEntity.getStatusCode());
        Assertions.assertEquals(userDetailsRequestJson.getString("firstName"), createdUserDetails.getFirstName(),
                "Returned user  firstName seems to be incorrect");
        Assertions.assertEquals(userDetailsRequestJson.getString("lastName"), createdUserDetails.getLastName(),
                "Returned user  lastName seems to be incorrect");
        Assertions.assertEquals(userDetailsRequestJson.getString("email"), createdUserDetails.getEmail(),
                "Returned user  email seems to be incorrect");

        Assertions.assertFalse(createdUserDetails.getUserId().trim().isEmpty(),
                "User ID should not be empty");
    }
    @Test
    @DisplayName("/login works")
    @Order(3)
    void testUserLogin_whenValidCredentialsProvided_ReturnsJWTAuthorizationHeader() throws JSONException {
        //Arrange

        JSONObject loginCredentials = new JSONObject();
        loginCredentials.put("email", "edenbercier@gmail.com");
        loginCredentials.put("password", "12345678");

        HttpEntity<String> request = new HttpEntity<>(loginCredentials.toString());

        //Act
        ResponseEntity response = testRestTemplate.postForEntity("/users/login",
                request,
                null);

        authorizationToken = response.getHeaders().
                getValuesAsList(SecurityConstants.HEADER_STRING).get(0);
        //Assert
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(),
                "HTTP Status code should be 200");

        Assertions.assertNotNull(authorizationToken,
                "Response should contain Authorization Code");
        Assertions.assertNotNull(response.getHeaders().getValuesAsList("UserID").get(0),
                "Response should contain UserID in response header");
    }
    @Test
    @DisplayName("GET /Users requires JWT")
    @Order(4)
    void testGetUsers_whenMissingJWT_returns403(){
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity requestEntity = new HttpEntity(null, headers);

        // Act
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<UserRest>>() {
                });

        // Assert
        Assertions.assertEquals(HttpStatus.FORBIDDEN,response.getStatusCode(),
                "Http status code should have been returned");
    }


    @Test
    @Order(5)
    @DisplayName("GET /users works")
    void testGetUsers_whenValidJWTProvided_returnsUsers(){
        //Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authorizationToken);

        HttpEntity requestEntity = new HttpEntity(headers);

        //Act
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<UserRest>>() {
                });
        //Assert
        Assertions.assertEquals(HttpStatus.OK,response.getStatusCode(),
                "HTTP Status code should be 200");
        Assertions.assertTrue(response.getBody().size() == 1,
                "There should be one user in the list");

    }
}