package com.accommodation.platform.availability.repository;

import com.accommodation.platform.availability.entity.AvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlock, UUID> {

    @Query("""
            SELECT COUNT(ab) > 0 FROM AvailabilityBlock ab
            WHERE ab.bed.id = :bedId
              AND ab.startTime < :endTime
              AND ab.endTime > :startTime
            """)
    boolean existsOverlappingBlock(
            @Param("bedId") UUID bedId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
            SELECT DISTINCT ab.bed.id FROM AvailabilityBlock ab
            WHERE ab.bed.room.property.id = :propertyId
              AND ab.startTime < :endTime
              AND ab.endTime > :startTime
            """)
    Set<UUID> findBlockedBedIdsForProperty(
            @Param("propertyId") UUID propertyId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    List<AvailabilityBlock> findByBedIdOrderByStartTimeAsc(UUID bedId);
}
