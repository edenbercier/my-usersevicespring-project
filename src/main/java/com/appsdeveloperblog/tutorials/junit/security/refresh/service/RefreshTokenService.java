package com.appsdeveloperblog.tutorials.junit.security.refresh.service;

import com.appsdeveloperblog.tutorials.junit.security.refresh.entity.RefreshToken;
import com.appsdeveloperblog.tutorials.junit.security.refresh.repository.RefreshTokenRepository;
import com.appsdeveloperblog.userservice.exception.JwtAuthenticationException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

  @Service
  public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
      this.repository = repository;
    }

    public RefreshToken create(String userEmail) {
      RefreshToken token = new RefreshToken(
          UUID.randomUUID().toString(),
          userEmail,
          Instant.now().plus(REFRESH_TOKEN_TTL),
          false
      );

      return repository.save(token);
    }

    public RefreshToken validate(String token) {
      RefreshToken refreshToken = repository.findByToken(token)
                                            .orElseThrow(() ->
                                                new JwtAuthenticationException("Invalid refresh token"));

      if (refreshToken.isExpired() || refreshToken.isRevoked()) {
        throw new JwtAuthenticationException(
            "Refresh token expired or revoked");
      }

      return refreshToken;
    }

    public Optional<RefreshToken> find(String token) {
      return repository.findByToken(token);
    }

    public void revoke(RefreshToken token) {
      token.revoke();
      repository.save(token);
    }
    @Transactional
    public int cleanup() {
      return repository.deleteExpiredOrRevoked(Instant.now());
    }

  }

