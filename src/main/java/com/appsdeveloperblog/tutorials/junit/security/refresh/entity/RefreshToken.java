package com.appsdeveloperblog.tutorials.junit.security.refresh.entity;

    import jakarta.persistence.Entity;
    import jakarta.persistence.Id;
    import jakarta.persistence.Table;

    import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  private String token;

  private String userEmail;

  private Instant expiresAt;

  private boolean revoked;

  protected RefreshToken() {
    // JPA only
  }

  public RefreshToken(String token,
      String userEmail,
      Instant expiresAt,
      boolean revoked) {
    this.token = token;
    this.userEmail = userEmail;
    this.expiresAt = expiresAt;
    this.revoked = revoked;
  }

  public String getToken() {
    return token;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public void revoke() {
    this.revoked = true;
  }
}
