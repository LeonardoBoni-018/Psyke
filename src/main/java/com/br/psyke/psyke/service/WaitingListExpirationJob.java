package com.br.psyke.psyke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingListExpirationJob {

    private final WaitingListService waitingListService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireOffers() {
        log.info("Running waiting list expiration job");
        waitingListService.expireStaleOffers();
    }
}
