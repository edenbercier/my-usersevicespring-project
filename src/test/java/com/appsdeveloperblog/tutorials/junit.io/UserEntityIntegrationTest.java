package com.appsdeveloperblog.tutorials.junit.io;

import jakarta.persistence.PersistenceException;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public class UserEntityIntegrationTest {

  @Autowired
  private TestEntityManager testEntityManager;

  UserEntity userEntity = new UserEntity();

  @BeforeEach
  void setup() {

    userEntity.setUserId(UUID.randomUUID().toString());
    userEntity.setFirstName("Eden");
    userEntity.setLastName("Bercier");
    userEntity.setEmail("edenbercier@gmail.com");
    userEntity.setEncryptedPassword("12345678");
  }

  @Test
  void testUserEntity_whenValidUserDetailsProvided_returnsStoredUserDetails() {
    // Arrange

    // Act
    UserEntity storedUserEntity = testEntityManager.persistAndFlush(userEntity);
    // Assert

    Assertions.assertTrue(storedUserEntity.getId() > 0);
    Assertions.assertEquals(userEntity.getUserId(), storedUserEntity.getUserId());
    Assertions.assertEquals(userEntity.getFirstName(), storedUserEntity.getFirstName());
    Assertions.assertEquals(userEntity.getLastName(), storedUserEntity.getLastName());
    Assertions.assertEquals(userEntity.getEmail(), storedUserEntity.getEmail());
    Assertions.assertEquals(userEntity.getEncryptedPassword(),
        storedUserEntity.getEncryptedPassword());
  }

  @Test
  void testUserEntity_WhenFirstIsTooLong_ShouldThrowException() {
    // Arrange
    userEntity.setFirstName("12345678123456781234567812345678123456781234567812345678");
    // Act & Assert
    Assertions.assertThrows(PersistenceException.class, () -> {
      testEntityManager.persistAndFlush(userEntity);
    }, "Was Expecting a PersistenceException to be thrown");
  }

  @Test
    // Exception means the test has passed
  void testUserEntity_WhenUserIsNotUnique_ShouldThrowException() {
    // Arrange
    UserEntity user1 = new UserEntity();
    user1.setUserId("12345");
    user1.setEmail("eden@gmail.com");
    user1.setFirstName("Eden");
    user1.setLastName("Bercier");
    user1.setEncryptedPassword("12345678");

    testEntityManager.persistAndFlush(user1);
    testEntityManager.clear();

    UserEntity user2 = new UserEntity();
    user2.setUserId("12345");
    user2.setEmail("eden@gmail.com");
    user2.setFirstName("Eden");
    user2.setLastName("Bercier");
    user2.setEncryptedPassword("12345678");

    // Act & Assert
    Assertions.assertThrows(PersistenceException.class, () -> {
      testEntityManager.persistAndFlush(user2);
    });
  }

}

