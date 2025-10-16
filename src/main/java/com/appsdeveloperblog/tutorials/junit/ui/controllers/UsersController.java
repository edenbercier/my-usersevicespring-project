package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.lang.reflect.Type;
import java.util.List;

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
    @PostMapping
    public UserRest createUser(@RequestBody @Valid UserDetailsRequestModel userDetails) throws Exception {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = usersService.createUser(userDto);

        return modelMapper.map(createdUser, UserRest.class);
    }

    @GetMapping(path = "/{userId}")
    public ResponseEntity<UserRest> getUser(@PathVariable String userId) {
        UserDto userDto = usersService.getUserByUserId(userId);
        UserRest returnValue = modelMapper.map(userDto, UserRest.class);
        return ResponseEntity.ok(returnValue);
    }
    @GetMapping
    public List<UserRest> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "limit", defaultValue = "2") int limit) {
        List<UserDto> users = usersService.getUsers(page, limit);
        Type listType = new TypeToken<List<UserRest>>() {}.getType();
        return modelMapper.map(users, listType);
    }

    }



