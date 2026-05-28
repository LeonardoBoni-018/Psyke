package com.br.psyke.psyke.service;

import com.br.psyke.psyke.model.AvailabilityException;
import com.br.psyke.psyke.model.ProfessionalAvailability;
import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.repository.AvailabilityExceptionRepository;
import com.br.psyke.psyke.repository.ProfessionalAvailabilityRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SlotEngine {

    private final ProfessionalAvailabilityRepository availabilityRepo;
    private final AvailabilityExceptionRepository exceptionRepo;
    private final SlotRepository slotRepo;

    public List<LocalDateTime> generateFreeSlots(UUID professionalId, LocalDate from, LocalDate to, Integer durationMinutes) {
        var weekly = availabilityRepo.findByProfessionalIdAndActiveTrueOrderByDayOfWeek(professionalId);
        if (weekly.isEmpty()) return List.of();

        var exceptions = exceptionRepo.findActiveInRange(professionalId, from, to);
        var bookedSlots = slotRepo.findByProfessionalAndStatusInRange(
                professionalId, from.atStartOfDay(), to.atTime(LocalTime.MAX), Slot.SlotStatus.BOOKED);

        int sessionDuration = durationMinutes != null ? durationMinutes : weekly.get(0).getSessionDuration();
        var bookedTimes = new HashSet<LocalDateTime>();
        for (var s : bookedSlots) {
            var cursor = s.getStartTime();
            while (cursor.isBefore(s.getEndTime())) {
                bookedTimes.add(cursor);
                cursor = cursor.plusMinutes(sessionDuration);
            }
        }

        var result = new ArrayList<LocalDateTime>();
        var dateCursor = from;

        while (!dateCursor.isAfter(to)) {
            var exception = findExceptionForDate(exceptions, dateCursor);

            if (exception == null) {
                var dayOfWeek = dateCursor.getDayOfWeek().getValue() % 7;
                var avail = findAvailabilityForDay(weekly, dayOfWeek);
                if (avail != null) {
                    generateSlotsForDay(dateCursor, avail, sessionDuration, bookedTimes, result);
                }
            } else if (exception.getType() == AvailabilityException.ExceptionType.EXTRA_HOUR && exception.getStartTime() != null && exception.getEndTime() != null) {
                generateSlotsForRange(dateCursor, exception.getStartTime(), exception.getEndTime(), sessionDuration, bookedTimes, result);
            }

            dateCursor = dateCursor.plusDays(1);
        }

        return result;
    }

    private AvailabilityException findExceptionForDate(List<AvailabilityException> exceptions, LocalDate date) {
        for (var e : exceptions) {
            if (e.getExceptionDate().equals(date)) return e;
        }
        return null;
    }

    private ProfessionalAvailability findAvailabilityForDay(List<ProfessionalAvailability> availabilities, int dayOfWeek) {
        for (var a : availabilities) {
            if (a.getDayOfWeek() == dayOfWeek) return a;
        }
        return null;
    }

    private void generateSlotsForDay(LocalDate date, ProfessionalAvailability avail, int duration,
            Set<LocalDateTime> booked, List<LocalDateTime> result) {
        generateSlotsForRange(date, avail.getStartTime(), avail.getEndTime(), duration, booked, result);
    }

    private void generateSlotsForRange(LocalDate date, LocalTime start, LocalTime end, int duration,
            Set<LocalDateTime> booked, List<LocalDateTime> result) {
        var cursor = LocalDateTime.of(date, start);
        var endDt = LocalDateTime.of(date, end);

        while (!cursor.isAfter(endDt.minusMinutes(duration))) {
            if (!booked.contains(cursor)) {
                result.add(cursor);
            }
            cursor = cursor.plusMinutes(duration);
        }
    }
}
