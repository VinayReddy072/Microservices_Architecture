package com.labplatform.equipment.dto;
import com.labplatform.equipment.domain.EquipmentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEquipmentRequest {
    @NotBlank(message = "Equipment name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    @NotNull(message = "Category is required")
    private EquipmentCategory category;
    @Size(max = 100, message = "Serial number cannot exceed 100 characters")
    private String serialNumber;
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;
}
