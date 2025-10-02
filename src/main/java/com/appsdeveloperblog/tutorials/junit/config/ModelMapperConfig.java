package com.appsdeveloperblog.tutorials.junit.config;

import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.addMappings(new PropertyMap<UserDetailsRequestModel, UserDto>() {
            @Override
            protected void configure() {
                map().setEncryptedPassword(source.getRepeatPassword());
            }
        });

        mapper.addMappings(new PropertyMap<UserDto, UserRest>() {
            @Override
            protected void configure() {
                map().setPassword(source.getEncryptedPassword());
                map().setRepeatPassword(source.getEncryptedPassword()); // <-- Add this
            }
        });

        return mapper;
    }
}

