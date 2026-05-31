package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateRoomRequest;
import com.br.psyke.psyke.dto.UpdateRoomRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Room;
import com.br.psyke.psyke.repository.RoomRepository;
import com.br.psyke.psyke.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository rooms;
    private RoomService service;
    private UUID clinicId;
    private Room existingRoom;

    @BeforeEach
    void setUp() {
        service = new RoomService(rooms);
        clinicId = UUID.randomUUID();
        existingRoom = Room.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .name("Sala 01").color("#00FF00")
                .capacity(10).type(Room.RoomType.PHYSICAL)
                .active(true).build();
    }

    @Test
    void shouldCreateRoom() {
        var req = new CreateRoomRequest("Sala 02", "#FF0000", 5, "VIRTUAL");
        var saved = Room.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .name("Sala 02").color("#FF0000")
                .capacity(5).type(Room.RoomType.VIRTUAL)
                .active(true).build();

        when(rooms.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.name()).isEqualTo("Sala 02");
        assertThat(resp.type()).isEqualTo("VIRTUAL");
    }

    @Test
    void shouldCreateRoomWithDefaults() {
        var req = new CreateRoomRequest("Sala 03", null, null, null);
        var saved = Room.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .name("Sala 03").capacity(1).type(Room.RoomType.PHYSICAL).active(true).build();

        when(rooms.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.capacity()).isEqualTo(1);
        assertThat(resp.type()).isEqualTo("PHYSICAL");
    }

    @Test
    void shouldFindAllActiveRooms() {
        when(rooms.findByClinicIdAndActiveTrue(clinicId)).thenReturn(List.of(existingRoom));

        var resp = service.findAllActive(clinicId);
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).name()).isEqualTo("Sala 01");
    }

    @Test
    void shouldFindRoomById() {
        when(rooms.findById(existingRoom.getId())).thenReturn(Optional.of(existingRoom));

        var resp = service.findById(existingRoom.getId());
        assertThat(resp.name()).isEqualTo("Sala 01");
    }

    @Test
    void shouldThrowWhenRoomNotFound() {
        var id = UUID.randomUUID();
        when(rooms.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateRoom() {
        var id = existingRoom.getId();
        var req = new UpdateRoomRequest("Sala 01 Updated", "#0000FF", 8, null, null);

        when(rooms.findById(id)).thenReturn(Optional.of(existingRoom));
        when(rooms.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.name()).isEqualTo("Sala 01 Updated");
        assertThat(resp.color()).isEqualTo("#0000FF");
        assertThat(resp.capacity()).isEqualTo(8);
    }

    @Test
    void shouldDeactivateRoom() {
        var id = existingRoom.getId();
        when(rooms.findById(id)).thenReturn(Optional.of(existingRoom));
        when(rooms.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(id);
        assertThat(existingRoom.isActive()).isFalse();
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(rooms.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
