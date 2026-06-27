package com.labplatform.equipment.mapper;
import com.labplatform.equipment.domain.Equipment;
import com.labplatform.equipment.dto.CreateEquipmentRequest;
import com.labplatform.equipment.dto.EquipmentResponse;
import com.labplatform.equipment.dto.UpdateEquipmentRequest;
import org.mapstruct.*;
import java.util.List;
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EquipmentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "maintenanceRequired", ignore = true)
    @Mapping(target = "lastMaintenanceAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Equipment toEntity(CreateEquipmentRequest request);
    EquipmentResponse toResponse(Equipment equipment);
    List<EquipmentResponse> toResponseList(List<Equipment> equipment);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "lastMaintenanceAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(
            @MappingTarget Equipment equipment,
            UpdateEquipmentRequest request
    );
}
