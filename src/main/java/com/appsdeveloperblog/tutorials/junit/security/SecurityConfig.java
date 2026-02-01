package com.appsdeveloperblog.tutorials.junit.security;

import com.appsdeveloperblog.tutorials.junit.io.UsersRepository;
import com.appsdeveloperblog.tutorials.junit.security.refresh.service.RefreshTokenService;
import com.appsdeveloperblog.tutorials.junit.security.token.JwtService;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Autowired
  private UsersRepository usersRepository;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationConfiguration authConfig,
      UserDetailsService userDetailsService,
      JwtService jwtService,
      UsersService usersService,
      RefreshTokenService refreshTokenService)
      throws Exception {
    AuthenticationManager authenticationManager = authConfig.getAuthenticationManager();
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler())
                .authenticationEntryPoint(customAuthenticationEntryPoint())
            )
        .addFilter(new LoginAuthenticationFilter(authenticationManager, jwtService,usersService, refreshTokenService ))
        .addFilter(new JwtTokenValidationFilter(authenticationManager, jwtService));

    return http.build();
  }

  @Bean
  public AccessDeniedHandler customAccessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Access Denied\"}");
    };
  }
  @Bean
  public org.springframework.security.web.AuthenticationEntryPoint customAuthenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Unauthorized\"}");
    };
  }
  @Bean
  public PasswordEncoder passwordEncoder() {

    return new BCryptPasswordEncoder();
  }
}