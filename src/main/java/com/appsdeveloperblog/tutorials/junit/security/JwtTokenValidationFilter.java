package com.appsdeveloperblog.tutorials.junit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class JwtTokenValidationFilter extends BasicAuthenticationFilter {
  private final UserDetailsService userDetailsService;
  public JwtTokenValidationFilter(AuthenticationManager authManager,
      UserDetailsService userDetailsService) {
    super(authManager);
    this.userDetailsService = userDetailsService;
  }


  @Override
  protected void doFilterInternal(HttpServletRequest req,
      HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {

    String token = req.getHeader(SecurityConstants.HEADER_STRING);

    if (token != null && token.startsWith(SecurityConstants.TOKEN_PREFIX)) {
      UsernamePasswordAuthenticationToken authentication = getAuthentication(token);
      if (authentication != null) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    chain.doFilter(req, res);
  }

  private UsernamePasswordAuthenticationToken getAuthentication(String token) {
    try {
      token = token.replace(SecurityConstants.TOKEN_PREFIX, "").trim();

      Claims claims = Jwts.parser()
                          .setSigningKey(SecurityConstants.TOKEN_SECRET)
                          .parseClaimsJws(token)
                          .getBody();

      String email = claims.getSubject();
      String role = (String) claims.get("role");

      if (email != null && role != null) {
        logger.info(" Parsed user from JWT: " + email + ", role: " + role);

        return new UsernamePasswordAuthenticationToken(
            email,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
      }

    } catch (Exception e) {
      logger.info(" Failed to parse JWT: " + e.getMessage());
      e.printStackTrace();
    }

    return null;

  }
}