package com.labplatform.booking.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class CreateBookingRequest {
    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
