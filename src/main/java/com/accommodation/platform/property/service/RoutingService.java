package com.accommodation.platform.property.service;

import org.springframework.stereotype.Service;

/**
 * Clean abstraction for travel time estimation.
 * Uses Haversine-based urban estimate by default when no external routing provider is configured.
 */
@Service
public class RoutingService {

    public Integer estimateTravelMinutes(Double distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        // Conservative urban India estimate (~22 km/h average)
        return Math.max(5, (int) Math.ceil((distanceKm / 22.0) * 60.0));
    }
}
