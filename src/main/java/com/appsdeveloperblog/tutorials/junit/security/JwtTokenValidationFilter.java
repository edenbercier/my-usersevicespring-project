package com.appsdeveloperblog.tutorials.junit.security;

import com.appsdeveloperblog.tutorials.junit.security.token.JwtService;
import com.appsdeveloperblog.userservice.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class JwtTokenValidationFilter extends BasicAuthenticationFilter {
  private final JwtService jwtService;
  private static final Logger logger =
      LoggerFactory.getLogger(JwtTokenValidationFilter.class);

  public JwtTokenValidationFilter(AuthenticationManager authManager, JwtService jwtService)
    {
      super(authManager);
      this.jwtService = jwtService;
    }

  @Override
  protected void doFilterInternal(HttpServletRequest req,
      HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {

    String header = req.getHeader(SecurityConstants.HEADER_STRING);

    if (header == null || header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
      chain.doFilter(req, res);
      return;
    }
    try {
      UsernamePasswordAuthenticationToken authentication = getAuthentication(header);

      SecurityContextHolder
          .getContext()
          .setAuthentication(authentication);

    } catch (JwtAuthenticationException ex) {
      SecurityContextHolder.clearContext();
      throw ex;
    }

    chain.doFilter(req, res);
  }


  private UsernamePasswordAuthenticationToken getAuthentication(String header) {
    try {
      String token = header
          .replace(SecurityConstants.TOKEN_PREFIX, "")
          .trim();

      Claims claims = jwtService.parseClaims(token);


      String email = claims.getSubject();
      String role = (String) claims.get("role");

      if (email == null || role == null) {
        throw new JwtAuthenticationException("Missing email or role");
      }
      List<SimpleGrantedAuthority> authorities =
          RolePermissions
              .permissionsFor(List.of(role))
              .stream()
              .map(SimpleGrantedAuthority::new)   // permissions
              .collect(Collectors.toList());

      authorities.add(
          new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
      );

      logger.info(
          "Authenticated user={}, role={}, authorities={}",
          email,
          role,
          authorities
      );

      return new UsernamePasswordAuthenticationToken(
          email,
          null,
          authorities
      );

    } catch (Exception e) {
      throw new JwtAuthenticationException(
          "Invalid or expired JWT: " + e.getMessage()
      );
    }
  }
}