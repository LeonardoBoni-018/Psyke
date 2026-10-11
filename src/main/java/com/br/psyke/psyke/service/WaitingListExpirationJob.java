package com.br.psyke.psyke.service;

import com.br.psyke.psyke.repository.RefreshTokenRepository;
import com.br.psyke.psyke.security.TenantRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

/** Housekeeping jobs. Tenant data lives in per-tenant schemas, so tenant jobs run once per tenant. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingListExpirationJob {

    private final WaitingListService waitingListService;
    private final TenantRunner tenantRunner;
    private final RefreshTokenRepository refreshTokens;

    @Scheduled(cron = "0 0 * * * *")
    public void expireOffers() {
        log.info("Running waiting list expiration job");
        tenantRunner.forEachActiveTenant("expire-waiting-list-offers", t -> waitingListService.expireStaleOffers());
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        var removed = refreshTokens.deleteExpiredBefore(OffsetDateTime.now().minusDays(1));
        log.info("Purged {} expired refresh tokens", removed);
    }
}
