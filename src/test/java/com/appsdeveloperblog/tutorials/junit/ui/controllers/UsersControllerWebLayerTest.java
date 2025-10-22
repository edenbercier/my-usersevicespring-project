package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.appsdeveloperblog.tutorials.junit.io.UsersRepository;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.service.UsersServiceImpl;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = UsersController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
//@AutoConfigureMockMvc(addFilters = false)
@MockBean({UsersServiceImpl.class})
public class UsersControllerWebLayerTest {

  @MockBean
  UsersService usersService;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ModelMapper modelMapper;
  @MockBean
  private UsersRepository usersRepository;


  private UserDetailsRequestModel userDetailsRequestModel;

  @BeforeEach
  void setup() {
    userDetailsRequestModel = new UserDetailsRequestModel();
    userDetailsRequestModel.setFirstName("Eden");
    userDetailsRequestModel.setLastName("Bercier");
    userDetailsRequestModel.setEmail("edenbercier@gmail.com");
    userDetailsRequestModel.setPassword("12345678");
    userDetailsRequestModel.setRepeatPassword("12345678");
  }

  @AfterEach
  void teardown() {
    reset(usersService);
  }

  @Test
  @DisplayName("User can be created")
  void testCreateUser_WhenValidUserDetailsProvided_ReturnsCreateUserDetails() throws Exception {
    // Arrange
    userDetailsRequestModel.setFirstName("Eden");
    userDetailsRequestModel.setLastName("Bercier");
    userDetailsRequestModel.setEmail("edenbercier@gmail.com");
    userDetailsRequestModel.setPassword("12345678");
    userDetailsRequestModel.setRepeatPassword("12345678");

    when(usersService.createUser(any(UserDto.class))).thenAnswer(invocation -> {
      UserDto input = invocation.getArgument(0);
      input.setUserId(UUID.randomUUID().toString()); // simulate creation
      return input;
    });

    RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel));

    // Act
    MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

    verify(usersService).createUser(any(UserDto.class));

    String responseBodyAsString = mvcResult.getResponse().getContentAsString();
    UserRest createdUser = new ObjectMapper().readValue(responseBodyAsString, UserRest.class);

    // Assert
    Assertions.assertEquals(userDetailsRequestModel.getFirstName(),
        createdUser.getFirstName(), "The returned user first name is likely incorrect");

    Assertions.assertEquals(userDetailsRequestModel.getLastName(),
        createdUser.getLastName(), "The returned user last name is likely incorrect");

    Assertions.assertEquals(userDetailsRequestModel.getEmail(),
        createdUser.getEmail(), "The returned user email is likely incorrect");

    Assertions.assertEquals(userDetailsRequestModel.getPassword(),
        createdUser.getPassword(), "The returned user password is likely incorrect");

    Assertions.assertEquals(userDetailsRequestModel.getRepeatPassword(),
        createdUser.getRepeatPassword(), "The returned user repeat password is likely incorrect");

    Assertions.assertFalse(createdUser.getUserId().isEmpty(), "userId should not be empty");
  }

  @Test
  @DisplayName("First name is not empty")
  void testCreateUser_whenFirstNameIsNotProvided_Returns400StatusCode() throws Exception {
    // Arrange
    userDetailsRequestModel.setFirstName(null);

    when(usersService.createUser(any(UserDto.class))).thenAnswer(invocation -> {
      UserDto input = invocation.getArgument(0);
      input.setUserId(UUID.randomUUID().toString()); // simulate creation
      return input;
    });

    RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel));

    // Act
    MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

    // Assert
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(),
        mvcResult.getResponse().getStatus(),
        "Incorrect HTTP Status Code returned ");

  }

  @Test
  @DisplayName("First name cannot be shorter than 2 characters")
  void testCreateUser_whenFirstNameIsOnlyOneCharacter_returns400StatusCode() throws Exception {
    // Arrange
    userDetailsRequestModel.setFirstName("M");

    RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
        .with(csrf())
        .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON);

    // Act
    MvcResult result = mockMvc.perform(requestBuilder).andReturn();

    // Debug: print response status and body
    System.out.println("Response status: " + result.getResponse().getStatus());
    System.out.println("Response body: " + result.getResponse().getContentAsString());

    // Assert
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(),
        result.getResponse().getStatus(), "HTTP Status code is not set to 400");
  }

}

