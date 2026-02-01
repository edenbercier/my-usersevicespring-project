package com.appsdeveloperblog.tutorials.junit.security.jobs;

import com.appsdeveloperblog.tutorials.junit.security.refresh.service.RefreshTokenService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RefreshTokenCleanupJob {

  private final RefreshTokenService refreshTokenService;

  public RefreshTokenCleanupJob(RefreshTokenService refreshTokenService) {
    this.refreshTokenService = refreshTokenService;
  }

  @Scheduled(cron = "0 0 * * * *") // every hour
  public void cleanup() {
    refreshTokenService.cleanup();
  }
}
