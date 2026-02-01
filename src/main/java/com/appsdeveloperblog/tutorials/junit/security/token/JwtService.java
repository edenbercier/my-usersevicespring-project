package com.appsdeveloperblog.tutorials.junit.security.token;


    import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
    import io.jsonwebtoken.Claims;
    import io.jsonwebtoken.Jwts;
    import io.jsonwebtoken.SignatureAlgorithm;
    import java.util.Date;
    import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final long ACCESS_TOKEN_EXPIRATION_MS = 10 * 60 * 1000L;


  public String generateAccessToken(String email, Object role) {
    return Jwts.builder()
               .setSubject(email)
               .claim("role", role)
               .setExpiration(
                   new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS)
               )
               .signWith(SignatureAlgorithm.HS512, SecurityConstants.TOKEN_SECRET)
               .compact();
  }
  public Claims parseClaims(String token) {
    return Jwts.parser()
               .setSigningKey(SecurityConstants.TOKEN_SECRET)
               .parseClaimsJws(token)
               .getBody();
  }

}
