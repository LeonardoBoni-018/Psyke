package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateNotificationConfigRequest;
import com.br.psyke.psyke.dto.NotificationConfigResponse;
import com.br.psyke.psyke.dto.UpdateNotificationConfigRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.NotificationConfig;
import com.br.psyke.psyke.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationConfigService {

    private final NotificationConfigRepository configs;

    public NotificationConfigResponse findByUserId(UUID userId) {
        return configs.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification config not found for user"));
    }

    @Transactional
    public NotificationConfigResponse create(CreateNotificationConfigRequest req) {
        var config = NotificationConfig.builder()
                .userId(req.userId())
                .emailEnabled(req.emailEnabled() != null ? req.emailEnabled() : true)
                .smsEnabled(req.smsEnabled() != null ? req.smsEnabled() : false)
                .pushEnabled(req.pushEnabled() != null ? req.pushEnabled() : false)
                .remindBeforeMinutes(req.remindBeforeMinutes() != null ? req.remindBeforeMinutes() : 1440)
                .notifyCancellations(req.notifyCancellations() != null ? req.notifyCancellations() : true)
                .notifyConfirmations(req.notifyConfirmations() != null ? req.notifyConfirmations() : true)
                .notifyChanges(req.notifyChanges() != null ? req.notifyChanges() : true)
                .build();
        config = configs.save(config);
        return toResponse(config);
    }

    @Transactional
    public NotificationConfigResponse update(UUID id, UpdateNotificationConfigRequest req) {
        var config = configs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification config not found"));

        if (req.emailEnabled() != null) config.setEmailEnabled(req.emailEnabled());
        if (req.smsEnabled() != null) config.setSmsEnabled(req.smsEnabled());
        if (req.pushEnabled() != null) config.setPushEnabled(req.pushEnabled());
        if (req.remindBeforeMinutes() != null) config.setRemindBeforeMinutes(req.remindBeforeMinutes());
        if (req.notifyCancellations() != null) config.setNotifyCancellations(req.notifyCancellations());
        if (req.notifyConfirmations() != null) config.setNotifyConfirmations(req.notifyConfirmations());
        if (req.notifyChanges() != null) config.setNotifyChanges(req.notifyChanges());

        config = configs.save(config);
        return toResponse(config);
    }

    @Transactional
    public void delete(UUID id) {
        var config = configs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification config not found"));
        configs.delete(config);
    }

    private NotificationConfigResponse toResponse(NotificationConfig c) {
        return new NotificationConfigResponse(
                c.getId().toString(), c.getUserId(),
                c.isEmailEnabled(), c.isSmsEnabled(), c.isPushEnabled(),
                c.getRemindBeforeMinutes(),
                c.isNotifyCancellations(), c.isNotifyConfirmations(), c.isNotifyChanges(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
