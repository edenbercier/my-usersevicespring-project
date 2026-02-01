package com.appsdeveloperblog.tutorials.junit.security.web;

    import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
    import com.appsdeveloperblog.tutorials.junit.security.refresh.entity.RefreshToken;
    import com.appsdeveloperblog.tutorials.junit.security.refresh.service.RefreshTokenService;
    import com.appsdeveloperblog.tutorials.junit.security.token.JwtService;
    import com.appsdeveloperblog.tutorials.junit.service.UsersService;
    import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
    import com.appsdeveloperblog.userservice.exception.JwtAuthenticationException;
    import java.time.Duration;
    import java.util.Map;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.ResponseCookie;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.CookieValue;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/auth")
public class AuthTokenController {
  private final UsersService usersService;

  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  @Value("${security.cookies.secure}")
  private boolean secureCookie;

  public AuthTokenController(UsersService usersService, JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.usersService = usersService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      @CookieValue(name = "refreshToken", required = false) String token) {

    if (token == null) {
      throw new JwtAuthenticationException("Missing refresh token");
    }


    // 1️⃣ Validate refresh token (identity proof)
    RefreshToken refreshToken =
        refreshTokenService.validate(token);

    String email = refreshToken.getUserEmail();

    // 2️⃣ Revoke old refresh token (rotation)
    refreshTokenService.revoke(refreshToken);

    // 3️⃣ Issue new refresh token
    RefreshToken newRefresh =
        refreshTokenService.create(email);

    // 4️⃣ 🔑 RE-DERIVE ROLE FROM SOURCE OF TRUTH
    UserDto userDto = usersService.getUser(email);

    // Match existing JWT expectations (STRING role)
    String role = userDto.getRoles().get(0);

    // 5️⃣ Issue new access token
    String newAccessToken =
        jwtService.generateAccessToken(email, role);

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
        .secure(secureCookie)
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
        .secure(secureCookie)
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(0)
        .build()
        .toString();
  }
}
