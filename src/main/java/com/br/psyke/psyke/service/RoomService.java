package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateRoomRequest;
import com.br.psyke.psyke.dto.RoomResponse;
import com.br.psyke.psyke.dto.UpdateRoomRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Room;
import com.br.psyke.psyke.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository rooms;

    public List<RoomResponse> findAllActive(UUID clinicId) {
        return rooms.findByClinicIdAndActiveTrue(clinicId).stream().map(this::toResponse).toList();
    }

    public RoomResponse findById(UUID id) {
        return rooms.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest req, UUID clinicId) {
        var room = Room.builder()
                .clinicId(clinicId)
                .name(req.name())
                .color(req.color())
                .capacity(req.capacity() != null ? req.capacity() : 1)
                .type(req.type() != null
                        ? Room.RoomType.valueOf(req.type())
                        : Room.RoomType.PHYSICAL)
                .build();
        room = rooms.save(room);
        return toResponse(room);
    }

    @Transactional
    public RoomResponse update(UUID id, UpdateRoomRequest req) {
        var room = rooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (req.name() != null) room.setName(req.name());
        if (req.color() != null) room.setColor(req.color());
        if (req.capacity() != null) room.setCapacity(req.capacity());
        if (req.type() != null) room.setType(Room.RoomType.valueOf(req.type()));
        if (req.active() != null) room.setActive(req.active());

        room = rooms.save(room);
        return toResponse(room);
    }

    @Transactional
    public void delete(UUID id) {
        var room = rooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        room.setActive(false);
        rooms.save(room);
    }

    private RoomResponse toResponse(Room r) {
        return new RoomResponse(
                r.getId().toString(), r.getName(), r.getColor(),
                r.getCapacity(), r.getType().name(), r.isActive(),
                r.getCreatedAt());
    }
}
