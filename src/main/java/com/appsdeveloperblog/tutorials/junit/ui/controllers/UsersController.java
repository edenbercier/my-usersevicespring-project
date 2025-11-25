package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import com.appsdeveloperblog.tutorials.junit.Permission;
import com.appsdeveloperblog.tutorials.junit.Rbac;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import com.appsdeveloperblog.userservice.exception.UsersServiceException;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.lang.reflect.Type;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/users")

public class UsersController {

  private final UsersService usersService;
  private final ModelMapper modelMapper;

  @Autowired
  public UsersController(UsersService usersService, ModelMapper modelMapper) {
    this.usersService = usersService;
    this.modelMapper = modelMapper;
  }
  @PostMapping("/register")
  public UserRest registerUser(@RequestBody @Valid UserDetailsRequestModel userDetails) {
    if (!userDetails.getPassword().equals(userDetails.getRepeatPassword())) {
      throw new UsersServiceException("Passwords do not match");
    }
    UserDto userDto = modelMapper.map(userDetails, UserDto.class);

    // Assign default role for public users
    userDto.setRole("viewer");

    UserDto createdUser = usersService.createUser(userDto);

    return modelMapper.map(createdUser, UserRest.class);

  }
//  @PostMapping
//  public UserRest createUserForAdmin(@RequestBody @Valid UserDetailsRequestModel userDetails)
//      throws Exception {
//    UserDto userDto = modelMapper.map(userDetails, UserDto.class);
//
//    UserDto createdUser = usersService.createUser(userDto);
//
//    return modelMapper.map(createdUser, UserRest.class);
//  }
@PostMapping
public ResponseEntity<?> createUserAsAdmin(
    @RequestBody @Valid UserDetailsRequestModel userDetails,
    @AuthenticationPrincipal UserDetails currentUser // ← who is logged in
) {
  // 1️ Get role of current logged-in user
  String requesterRole = currentUser.getAuthorities().iterator().next().getAuthority();

  // 2️ RBAC: Block if they don’t have permission
  if (!Rbac.hasPermission(requesterRole, Permission.USER_CREATE)) {
    return ResponseEntity.status(403).body("Access denied");
  }

  // 3️ Map the request body to your internal DTO object
  UserDto userDto = modelMapper.map(userDetails, UserDto.class);

  // 4️ Let admin assign the role (it’s part of the request body)
  userDto.setRole(userDetails.getRole());

  // 5️ Save the user
  UserDto createdUser = usersService.createUser(userDto);
  UserRest returnValue = modelMapper.map(createdUser, UserRest.class);

  // 6️ Return success
  return ResponseEntity.status(201).body(returnValue);
}


  @GetMapping("/{userId}")
  public ResponseEntity<?> getUser(
      @PathVariable String userId,
      @AuthenticationPrincipal UserDetails currentUser
  ) {
    String role = currentUser.getAuthorities().iterator().next().getAuthority();
    String currentEmail = currentUser.getUsername(); // or ID depending on your logic

    if (!Rbac.hasPermission(role, Permission.USER_READ) && !usersService.getUserByUserId(userId).getEmail().equals(currentEmail)) {
      return ResponseEntity.status(403).body("Access denied");
    }

    UserDto userDto = usersService.getUserByUserId(userId);
    UserRest returnValue = modelMapper.map(userDto, UserRest.class);
    return ResponseEntity.ok(returnValue);
  }


  @GetMapping
  public ResponseEntity<?> getUsers(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "limit", defaultValue = "2") int limit,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    String role = userDetails.getAuthorities()
                             .iterator()
                             .next()
                             .getAuthority(); // Extract role

    if (!Rbac.hasPermission(role, Permission.USER_READ)) {
      return ResponseEntity.status(403).body("Access denied");
    }

    List<UserDto> users = usersService.getUsers(page, limit);
    Type listType = new TypeToken<List<UserRest>>() {}.getType();
    List<UserRest> returnValue = modelMapper.map(users, listType);
    return ResponseEntity.ok(returnValue);
  }

}



