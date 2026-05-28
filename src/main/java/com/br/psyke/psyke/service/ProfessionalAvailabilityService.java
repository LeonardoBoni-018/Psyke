package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalAvailabilityService {

    private final ProfessionalAvailabilityRepository availabilityRepo;
    private final AvailabilityExceptionRepository exceptionRepo;
    private final SlotRepository slotRepo;
    private final SlotEngine slotEngine;

    public List<SlotResponse> getSlots(UUID professionalId, LocalDate from, LocalDate to, Integer duration) {
        var freeSlots = slotEngine.generateFreeSlots(professionalId, from, to, duration);
        return freeSlots.stream()
                .map(dt -> new SlotResponse(null, professionalId, dt.toString(), dt.plusMinutes(duration != null ? duration : 50).toString(), "FREE"))
                .toList();
    }

    public List<AvailabilityResponse> listAvailability(UUID professionalId) {
        return availabilityRepo.findByProfessionalIdAndActiveTrueOrderByDayOfWeek(professionalId)
                .stream().map(this::toAvailabilityResponse).toList();
    }

    @Transactional
    public AvailabilityResponse createAvailability(UUID professionalId, AvailabilityRequest req) {
        var entity = ProfessionalAvailability.builder()
                .professionalId(professionalId)
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .sessionDuration(req.sessionDuration())
                .active(true)
                .build();
        entity = availabilityRepo.save(entity);
        return toAvailabilityResponse(entity);
    }

    @Transactional
    public AvailabilityResponse updateAvailability(UUID id, AvailabilityRequest req) {
        var entity = availabilityRepo.findById(id).orElseThrow();
        entity.setDayOfWeek(req.dayOfWeek());
        entity.setStartTime(req.startTime());
        entity.setEndTime(req.endTime());
        entity.setSessionDuration(req.sessionDuration());
        entity = availabilityRepo.save(entity);
        return toAvailabilityResponse(entity);
    }

    @Transactional
    public void deleteAvailability(UUID id) {
        availabilityRepo.deleteById(id);
    }

    public List<ExceptionResponse> listExceptions(UUID professionalId) {
        return exceptionRepo.findByProfessionalIdAndActiveTrue(professionalId)
                .stream().map(this::toExceptionResponse).toList();
    }

    @Transactional
    public ExceptionResponse createException(UUID professionalId, ExceptionRequest req) {
        var entity = AvailabilityException.builder()
                .professionalId(professionalId)
                .exceptionDate(req.exceptionDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .type(AvailabilityException.ExceptionType.valueOf(req.type()))
                .reason(req.reason())
                .active(true)
                .build();
        entity = exceptionRepo.save(entity);
        return toExceptionResponse(entity);
    }

    @Transactional
    public void deleteException(UUID id) {
        var ex = exceptionRepo.findById(id).orElseThrow();
        ex.setActive(false);
    }

    private AvailabilityResponse toAvailabilityResponse(ProfessionalAvailability a) {
        return new AvailabilityResponse(a.getId(), a.getDayOfWeek(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getSessionDuration(), a.isActive());
    }

    private ExceptionResponse toExceptionResponse(AvailabilityException e) {
        return new ExceptionResponse(e.getId(), e.getProfessionalId(),
                e.getExceptionDate().toString(),
                e.getStartTime() != null ? e.getStartTime().toString() : null,
                e.getEndTime() != null ? e.getEndTime().toString() : null,
                e.getType().name(), e.getReason(), e.isActive());
    }
}
