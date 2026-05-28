package com.br.psyke.psyke.service;

import com.br.psyke.psyke.model.CancellationLog;
import com.br.psyke.psyke.repository.CancellationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationLogService {

    private final CancellationLogRepository repo;

    @Transactional
    public void log(UUID appointmentId, UUID cancelledBy, String reason, String role) {
        repo.save(CancellationLog.builder()
                .appointmentId(appointmentId)
                .cancelledBy(cancelledBy)
                .reason(reason)
                .cancelledByRole(role)
                .build());
    }
}
