package com.labplatform.common.event;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String CURRENT_VERSION = "1.0";
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventVersion = CURRENT_VERSION;
    private Long bookingId;
    private Long equipmentId;
    private String userId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();
    private String traceId;
}
