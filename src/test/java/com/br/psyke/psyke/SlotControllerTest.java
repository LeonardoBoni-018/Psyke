package com.br.psyke.psyke;

import com.br.psyke.psyke.service.SlotEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotControllerTest {

    @Mock private SlotEngine slotEngine;

    @Test
    void shouldReturnEmptySlotsWhenNoAvailability() {
        var profId = UUID.randomUUID();
        var today = LocalDate.now();
        when(slotEngine.generateFreeSlots(profId, today, today, null)).thenReturn(List.of());

        var result = slotEngine.generateFreeSlots(profId, today, today, null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSlotsWhenAvailable() {
        var profId = UUID.randomUUID();
        var today = LocalDate.now();
        var slot1 = LocalDateTime.now().withHour(9).withMinute(0);
        var slot2 = LocalDateTime.now().withHour(10).withMinute(0);
        when(slotEngine.generateFreeSlots(profId, today, today, 50)).thenReturn(List.of(slot1, slot2));

        var result = slotEngine.generateFreeSlots(profId, today, today, 50);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(slot1);
    }

    @Test
    void shouldRespectCustomDuration() {
        var profId = UUID.randomUUID();
        var today = LocalDate.now();
        var slot = LocalDateTime.now().withHour(9).withMinute(0);
        when(slotEngine.generateFreeSlots(profId, today, today, 30)).thenReturn(List.of(slot));
        when(slotEngine.generateFreeSlots(profId, today, today, 60)).thenReturn(List.of());

        assertThat(slotEngine.generateFreeSlots(profId, today, today, 30)).hasSize(1);
        assertThat(slotEngine.generateFreeSlots(profId, today, today, 60)).isEmpty();
    }
}
