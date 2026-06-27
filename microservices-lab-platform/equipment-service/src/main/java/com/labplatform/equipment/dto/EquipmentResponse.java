package com.labplatform.equipment.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.labplatform.equipment.domain.EquipmentCategory;
import com.labplatform.equipment.domain.EquipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {
    private Long id;
    private String name;
    private String description;
    private EquipmentCategory category;
    private EquipmentStatus status;
    private String serialNumber;
    private String location;
    private Integer usageCount;
    private Boolean maintenanceRequired;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMaintenanceAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    public boolean isBookable() {
        return status == EquipmentStatus.AVAILABLE && !Boolean.TRUE.equals(maintenanceRequired);
    }
}
