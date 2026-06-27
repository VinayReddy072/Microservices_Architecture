package com.labplatform.booking.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.labplatform.booking.domain.BookingStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequest {
    @Future(message = "Start time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @Future(message = "End time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private BookingStatus status;
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
