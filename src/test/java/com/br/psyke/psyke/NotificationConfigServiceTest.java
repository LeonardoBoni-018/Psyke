package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateNotificationConfigRequest;
import com.br.psyke.psyke.dto.UpdateNotificationConfigRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.NotificationConfig;
import com.br.psyke.psyke.repository.NotificationConfigRepository;
import com.br.psyke.psyke.service.NotificationConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConfigServiceTest {

    @Mock private NotificationConfigRepository configs;
    private NotificationConfigService service;
    private UUID userId;
    private NotificationConfig existingConfig;

    @BeforeEach
    void setUp() {
        service = new NotificationConfigService(configs);
        userId = UUID.randomUUID();
        existingConfig = NotificationConfig.builder()
                .id(UUID.randomUUID()).userId(userId)
                .emailEnabled(true).smsEnabled(false)
                .remindBeforeMinutes(1440).notifyCancellations(true)
                .build();
    }

    @Test
    void shouldCreateConfig() {
        var req = new CreateNotificationConfigRequest(userId, true, true, false, 60, true, true, true);
        var saved = NotificationConfig.builder().id(UUID.randomUUID()).userId(userId)
                .emailEnabled(true).smsEnabled(true).remindBeforeMinutes(60)
                .notifyCancellations(true).notifyConfirmations(true).notifyChanges(true).build();

        when(configs.save(any())).thenReturn(saved);

        var resp = service.create(req);
        assertThat(resp.emailEnabled()).isTrue();
        assertThat(resp.smsEnabled()).isTrue();
        assertThat(resp.remindBeforeMinutes()).isEqualTo(60);
    }

    @Test
    void shouldFindByUserId() {
        when(configs.findByUserId(userId)).thenReturn(Optional.of(existingConfig));

        var resp = service.findByUserId(userId);
        assertThat(resp.emailEnabled()).isTrue();
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(configs.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateConfig() {
        var id = existingConfig.getId();
        var req = new UpdateNotificationConfigRequest(false, true, true, 30, true, true, true);

        when(configs.findById(id)).thenReturn(Optional.of(existingConfig));
        when(configs.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.emailEnabled()).isFalse();
        assertThat(resp.smsEnabled()).isTrue();
        assertThat(resp.remindBeforeMinutes()).isEqualTo(30);
    }

    @Test
    void shouldThrowOnUpdateWhenNotFound() {
        var id = UUID.randomUUID();
        when(configs.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateNotificationConfigRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteConfig() {
        var id = existingConfig.getId();
        when(configs.findById(id)).thenReturn(Optional.of(existingConfig));

        service.delete(id);
        verify(configs).delete(existingConfig);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(configs.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
