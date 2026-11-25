package com.accommodation.platform.bed.repository;

import com.accommodation.platform.bed.entity.Bed;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BedRepository extends JpaRepository<Bed, UUID> {
    List<Bed> findByRoomIdOrderByBedNumberAsc(UUID roomId);
    List<Bed> findByRoomPropertyId(UUID propertyId);
    long countByRoomId(UUID roomId);
    boolean existsByRoomIdAndBedNumberIgnoreCase(UUID roomId, String bedNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b JOIN FETCH b.room r JOIN FETCH r.property p WHERE b.id = :bedId")
    Optional<Bed> findByIdForUpdate(@Param("bedId") UUID bedId);
}
