package com.appsdeveloperblog.tutorials.junit.modelmapperconfig;

import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper mapper = new ModelMapper();

    // Map incoming request to internal DTO
    mapper.addMappings(new PropertyMap<UserDetailsRequestModel, UserDto>() {
      @Override
      protected void configure() {
        // Custom mapping if needed (optional): for example, map repeatPassword into encryptedPassword temporarily
        // Normally, password encryption is done in the service layer, not here
        // But here's an example if you're using repeatPassword as raw input
        map().setPassword(source.getPassword()); // Set password field (to be encrypted later)
      }
    });

    // Map internal DTO to outward-facing REST response
    mapper.addMappings(new PropertyMap<UserDto, UserRest>() {
      @Override
      protected void configure() {
        // Do NOT expose password fields in response
        skip(destination.getPassword());
        skip(destination.getRepeatPassword());
      }
    });

    return mapper;
  }
}
