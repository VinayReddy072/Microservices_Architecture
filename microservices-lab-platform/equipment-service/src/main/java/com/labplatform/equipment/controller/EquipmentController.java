package com.labplatform.equipment.controller;
import com.labplatform.common.dto.ApiResponse;
import com.labplatform.equipment.dto.AvailabilityResponse;
import com.labplatform.equipment.dto.CreateEquipmentRequest;
import com.labplatform.equipment.dto.EquipmentResponse;
import com.labplatform.equipment.dto.UpdateEquipmentRequest;
import com.labplatform.equipment.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {
    private final EquipmentService equipmentService;
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(
            @Valid @RequestBody CreateEquipmentRequest request) {
        log.info("POST /api/equipment — {}", request.getName());
        EquipmentResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment created successfully", equipment));
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getAllEquipment() {
        log.debug("GET /api/equipment");
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getAllEquipment()));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipmentById(@PathVariable Long id) {
        log.debug("GET /api/equipment/{}", id);
        return ResponseEntity.ok(ApiResponse.success(equipmentService.getEquipmentById(id)));
    }
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@PathVariable Long id) {
        log.debug("GET /api/equipment/{}/availability", id);
        return ResponseEntity.ok(equipmentService.checkAvailability(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEquipmentRequest request) {
        log.info("PUT /api/equipment/{}", id);
        EquipmentResponse updated = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Equipment updated successfully", updated));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        log.info("DELETE /api/equipment/{}", id);
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build(); 
    }
    @PostMapping("/{id}/maintenance/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EquipmentResponse>> completeMaintenance(@PathVariable Long id) {
        log.info("POST /api/equipment/{}/maintenance/complete", id);
        EquipmentResponse updated = equipmentService.completeMaintenance(id);
        return ResponseEntity.ok(ApiResponse.success("Maintenance completed — equipment is available", updated));
    }
}
