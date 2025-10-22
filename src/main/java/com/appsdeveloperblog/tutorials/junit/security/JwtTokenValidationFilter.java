package com.appsdeveloperblog.tutorials.junit.security;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class JwtTokenValidationFilter extends BasicAuthenticationFilter {

  public JwtTokenValidationFilter(AuthenticationManager authManager) {
    super(authManager);
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

      // Parse the token and extract subject (username/email)
      String user = Jwts.parser()
          .setSigningKey(SecurityConstants.TOKEN_SECRET)
          .parseClaimsJws(token)
          .getBody()
          .getSubject();

      if (user != null) {
        logger.info("✅ Parsed user from JWT: " + user);
        return new UsernamePasswordAuthenticationToken(user, null, null);
      }
    } catch (Exception e) {
      logger.info("❌ Failed to parse JWT: " + e.getMessage());
      e.printStackTrace();
    }

    return null;
  }
}