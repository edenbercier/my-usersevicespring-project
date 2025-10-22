package com.appsdeveloperblog.tutorials.junit.service;

import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UsersService extends UserDetailsService {

  UserDto createUser(UserDto user);

  List<UserDto> getUsers(int page, int limit);

  UserDto getUser(String email);

  UserDto getUserByUserId(String userId);
}
