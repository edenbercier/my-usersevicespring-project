package com.appsdeveloperblog.tutorials.junit.security.com.appsdeveloperblog.tutorials.junit.security;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.security.refresh.entity.RefreshToken;
import com.appsdeveloperblog.tutorials.junit.security.refresh.service.RefreshTokenService;
import com.appsdeveloperblog.tutorials.junit.security.token.JwtService;
import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.userservice.exception.JwtAuthenticationException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/auth")
public class AuthTokenController {
private final UsersService usersService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  @Value("${security.cookies.secure}")
  private boolean secureCookie;


  public AuthController(JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      @CookieValue(name = "refreshToken", required = false) String token) {

    if (token == null) {
      throw new JwtAuthenticationException("Missing refresh token");
    }

    RefreshToken refreshToken =
        refreshTokenService.validate(token);

    refreshTokenService.revoke(refreshToken);

    RefreshToken newRefresh =
        refreshTokenService.create(refreshToken.getUserEmail());

    String newAccessToken =
        jwtService.generateAccessToken(refreshToken.getUserEmail(), role);

    return ResponseEntity.ok()
                         .header(HttpHeaders.SET_COOKIE, buildCookie(newRefresh))
                         .body(Map.of("token", SecurityConstants.TOKEN_PREFIX + newAccessToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = "refreshToken", required = false) String token) {

    if (token != null) {
      refreshTokenService.find(token)
                         .ifPresent(refreshTokenService::revoke);
    }

    return ResponseEntity.ok()
                         .header(HttpHeaders.SET_COOKIE, deleteCookie())
                         .build();
  }

  private String buildCookie(RefreshToken token) {
    return ResponseCookie
        .from("refreshToken", token.getToken())
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(Duration.ofDays(14))
        .build()
        .toString();
  }

  private String deleteCookie() {
    return ResponseCookie
        .from("refreshToken", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(0)
        .build()
        .toString();
  }
}

