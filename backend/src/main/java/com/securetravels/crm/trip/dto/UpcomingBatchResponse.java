package com.securetravels.crm.trip.dto;

import com.securetravels.crm.trip.Batch;
import com.securetravels.crm.trip.CapacityColor;
import com.securetravels.crm.trip.Trip;

import java.time.LocalDate;
import java.util.UUID;

/** Departure row for calendar / dashboard upcoming lists. */
public record UpcomingBatchResponse(
        UUID id,
        UUID tripId,
        String tripName,
        Trip.Category tripCategory,
        LocalDate departureDate,
        int maxCapacity,
        int seatsBooked,
        int available,
        int fillPercent,
        CapacityColor capacityColor,
        String guideName,
        Batch.Status status
) {
}
