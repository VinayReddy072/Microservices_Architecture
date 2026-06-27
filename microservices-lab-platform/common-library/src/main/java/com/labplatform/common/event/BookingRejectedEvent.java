package com.labplatform.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when an asynchronous compensation is required.
 * Specifically used to handle split-brain race conditions where a booking
 * is created for an equipment that reached its maintenance threshold before
 * the booking was fully processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRejectedEvent {
    private Long bookingId;
    private Long equipmentId;
    private String reason;
}
