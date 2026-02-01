package com.appsdeveloperblog.tutorials.junit.security;

import com.appsdeveloperblog.tutorials.junit.security.refresh.entity.RefreshToken;
import com.appsdeveloperblog.tutorials.junit.security.refresh.service.RefreshTokenService;
import com.appsdeveloperblog.tutorials.junit.security.token.JwtService;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.shared.SpringApplicationContext;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UsersService usersService;
  private final RefreshTokenService refreshTokenService;

  public LoginAuthenticationFilter(
      AuthenticationManager authenticationManager,JwtService jwtService, UsersService usersService, RefreshTokenService refreshTokenService
  ) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.usersService = usersService;
    this.refreshTokenService = refreshTokenService;
    setFilterProcessesUrl("/login");
  }


  @Override
  public Authentication attemptAuthentication(HttpServletRequest req,
      HttpServletResponse res) throws AuthenticationException {
    try {
      byte[] inputStreamBytes = StreamUtils.copyToByteArray(req.getInputStream());
      Map<String, String> jsonRequest = new ObjectMapper().readValue(inputStreamBytes, Map.class);

//            UserLoginRequestModel creds = new ObjectMapper()
//                    .readValue(jsonRequest.get("body"), UserLoginRequestModel.class);

      return authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              jsonRequest.get("email"),
              jsonRequest.get("password"),
              new ArrayList<>())
      );

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest req,
      HttpServletResponse res,
      FilterChain chain,
      Authentication auth) throws IOException {

    String userName = ((UserDetails) auth.getPrincipal()).getUsername();

    UserDto userDto = usersService.getUser(userName);

    String role = userDto.getRoles().get(0);

    String token = jwtService.generateAccessToken(
        userName,
        role
    );

    //  Build response JSON
    Map<String, Object> responseBody = Map.of(
        "userId", userDto.getUserId(),
        "token", SecurityConstants.TOKEN_PREFIX + token
    );

    //  Set content type and write JSON
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    new ObjectMapper().writeValue(res.getWriter(), responseBody);


    RefreshToken refreshToken =
        refreshTokenService.create(userName);

    ResponseCookie refreshCookie = ResponseCookie
        .from("refreshToken", refreshToken.getToken())
        .httpOnly(true)
        .secure(true)                  // false only for localhost HTTP
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(Duration.ofDays(14))
        .build();

    res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

  }

  }

