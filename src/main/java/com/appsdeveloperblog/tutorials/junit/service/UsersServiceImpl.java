package com.appsdeveloperblog.tutorials.junit.service;

import com.appsdeveloperblog.tutorials.junit.io.UserEntity;
import com.appsdeveloperblog.tutorials.junit.io.UsersRepository;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.userservice.exception.UsersServiceException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("usersService")
public class UsersServiceImpl implements UsersService {

  private UsersRepository usersRepository;
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  public UsersServiceImpl(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
    this.usersRepository = usersRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDto createUser(UserDto user) {

    if (usersRepository.findByEmail(user.getEmail()) != null) {
      throw new UsersServiceException("Record already exists");
    }

    UserEntity userEntity = modelMapper.map(user, UserEntity.class);

    String publicUserId = UUID.randomUUID().toString();
    userEntity.setUserId(publicUserId);
    userEntity.setEncryptedPassword("{bcrypt}" + passwordEncoder.encode(user.getPassword()));

    UserEntity storedUserDetails = usersRepository.save(userEntity);

    UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);

    return returnValue;
  }

  @Override
  public List<UserDto> getUsers(int page, int limit) {
    List<UserDto> returnValue = new ArrayList<>();

    if (page > 0) {
      page -= 1;
    }

    Pageable pageableRequest = PageRequest.of(page, limit);

    Page<UserEntity> usersPage = usersRepository.findAll(pageableRequest);
    List<UserEntity> users = usersPage.getContent();

    Type listType = new TypeToken<List<UserDto>>() {
    }.getType();
    returnValue = new ModelMapper().map(users, listType);

    return returnValue;
  }

  @Override
  public UserDto getUser(String email) {
    UserEntity userEntity = usersRepository.findByEmail(email);

    if (userEntity == null) {
      throw new UsernameNotFoundException(email);
    }

    UserDto returnValue = new UserDto();
    BeanUtils.copyProperties(userEntity, returnValue);

    return returnValue;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    UserEntity userEntity = usersRepository.findByEmail(email);

    if (userEntity == null) {
      throw new UsernameNotFoundException(email);
    }

    return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), new ArrayList<>());
  }

  @Override
  public UserDto getUserByUserId(String userId) {
    UserEntity userEntity = usersRepository.findByUserId(userId);

    if (userEntity == null) {
      throw new UsernameNotFoundException("User ID: " + userId);
    }

    return modelMapper.map(userEntity, UserDto.class);
  }
}
