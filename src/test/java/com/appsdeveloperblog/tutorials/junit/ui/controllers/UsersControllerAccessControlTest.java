package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class UsersControllerAccessControlTest {
  @Autowired
private MockMvc mockMvc;
  @WithMockUser(username = "viewer@example.com", roles = {"USER"})
  @Test
  void nonAdminCannotCreateAdminUser() throws Exception {
    String payload = """
            {
              "firstName": "Evil",
              "lastName": "Hacker",
              "email": "evil@hackers.com",
              "password": "Secret123",
              "repeatPassword": "Secret123",
              "roles": ["admin"]
            }
        """;

    mockMvc.perform(post("/users")
               .contentType(MediaType.APPLICATION_JSON) // header
               .content(payload))                       // body
           .andExpect(status().isForbidden());}
}
