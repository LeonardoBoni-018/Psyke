package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByClinicIdAndActiveTrue(UUID clinicId);
}
