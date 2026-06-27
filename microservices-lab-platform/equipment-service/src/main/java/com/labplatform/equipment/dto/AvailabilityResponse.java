package com.labplatform.equipment.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean available;
    private Long equipmentId;
    private String reason;
    private String status;
    public static AvailabilityResponse available(Long equipmentId) {
        return AvailabilityResponse.builder()
                .available(true)
                .equipmentId(equipmentId)
                .reason("Equipment is available for booking")
                .status("AVAILABLE")
                .build();
    }
    public static AvailabilityResponse unavailable(Long equipmentId, String reason, String status) {
        return AvailabilityResponse.builder()
                .available(false)
                .equipmentId(equipmentId)
                .reason(reason)
                .status(status)
                .build();
    }
}
