package com.appsdeveloperblog.tutorials.junit.security.refresh.repository;
import com.appsdeveloperblog.tutorials.junit.security.refresh.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository

public interface RefreshTokenRepository   extends JpaRepository<RefreshToken, String>  {
  Optional<RefreshToken> findByToken(String token);

  void deleteByUserEmail(String userEmail);
  @Modifying
  @Query("""
    delete from RefreshToken rt
    where rt.expiresAt < :now or rt.revoked = true
""")
  int deleteExpiredOrRevoked(@Param("now") Instant now);

}
