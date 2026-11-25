package com.accommodation.platform.room.repository;

import com.accommodation.platform.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByPropertyIdOrderByFloorAscRoomNumberAsc(UUID propertyId);
    Optional<Room> findByIdAndPropertyId(UUID id, UUID propertyId);
    boolean existsByPropertyIdAndRoomNumberIgnoreCase(UUID propertyId, String roomNumber);
}
