package com.appsdeveloperblog.tutorials.junit.io;


import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public class UsersRepositoryTest {

  @Autowired
  TestEntityManager testEntityManager;
  @Autowired
  UsersRepository usersRepository;
  private UserEntity userEntity;
  private UserEntity userEntity2;

  @BeforeEach
  void setup() {
    // First User
    userEntity = new UserEntity();
    userEntity.setFirstName("Eden");
    userEntity.setLastName("Bercier");
    userEntity.setEmail("email1");
    userEntity.setEncryptedPassword("12345678");
    userEntity.setUserId(UUID.randomUUID().toString());
    testEntityManager.persistAndFlush(userEntity);

    // Second User
    userEntity2 = new UserEntity();
    userEntity2.setFirstName("John");
    userEntity2.setLastName("Kent");
    userEntity2.setEmail("email2");
    userEntity2.setEncryptedPassword("abcdefgh");
    userEntity2.setUserId(UUID.randomUUID().toString());
    testEntityManager.persistAndFlush(userEntity2);
  }

  @Test
  void testFindByEmail_whenGivenCorrectEmail_returnsUserEntity() {
    // Act
    UserEntity storedUser = usersRepository.findByEmail(userEntity.getEmail());

    // Assert
    Assertions.assertEquals(userEntity.getEmail(), storedUser.getEmail(),
        "Returned email does not match expected email value");

  }

  @Test
  void testFindByUser_whenGivenCorrectUserID_returnUserEntity() {
    // Act
    UserEntity storedUser = usersRepository.findByUserId(userEntity2.getUserId());
    // Assert
    Assertions.assertEquals(userEntity2.getUserId(), storedUser.getUserId(),
        "Returned User Id does not match expected user ID");

  }

  @Test
  void testFindUsersWithEmailEndsWith_whenGivenEmailDomain_returnsUsersWithGivenDomain() {
    // Arrange
    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(UUID.randomUUID().toString());
    userEntity.setEmail("test@gmail.com");
    userEntity.setEncryptedPassword("123456789");
    userEntity.setFirstName("Charles");
    userEntity.setLastName("Dickens");
    testEntityManager.persistAndFlush(userEntity);

    String emailDomainName = "%@gmail.com";

    // Act

    List<UserEntity> users = usersRepository.findUsersWithEmailEndingWith(emailDomainName);

    // Assert
    Assertions.assertEquals(1, users.size(), "Should be only one user with given email domain");
    Assertions.assertTrue(users.get(0).getEmail().endsWith("@gmail.com"));
  }
}