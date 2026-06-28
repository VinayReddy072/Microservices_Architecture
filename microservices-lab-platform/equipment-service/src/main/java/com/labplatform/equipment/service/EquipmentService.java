package com.labplatform.equipment.service;
import com.labplatform.equipment.domain.Equipment;
import com.labplatform.equipment.domain.EquipmentStatus;
import com.labplatform.equipment.dto.AvailabilityResponse;
import com.labplatform.equipment.dto.CreateEquipmentRequest;
import com.labplatform.equipment.dto.EquipmentResponse;
import com.labplatform.equipment.dto.UpdateEquipmentRequest;
import com.labplatform.equipment.exception.EquipmentNotFoundException;
import com.labplatform.equipment.mapper.EquipmentMapper;
import com.labplatform.equipment.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;
    @Value("${equipment.maintenance.usage-threshold:10}")
    private int maintenanceThreshold;
    @Transactional
    public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
        log.info("Creating equipment: name={} category={}", request.getName(), request.getCategory());
        if (request.getSerialNumber() != null &&
                equipmentRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new IllegalArgumentException("Equipment with serial number '" +
                    request.getSerialNumber() + "' already exists");
        }
        Equipment equipment = equipmentMapper.toEntity(request);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        Equipment saved = equipmentRepository.save(equipment);
        log.info("Equipment created: id={}", saved.getId());
        return equipmentMapper.toResponse(saved);
    }
    public List<EquipmentResponse> getAllEquipment() {
        return equipmentMapper.toResponseList(equipmentRepository.findAll());
    }
    public EquipmentResponse getEquipmentById(Long id) {
        return equipmentMapper.toResponse(findById(id));
    }
    @Transactional
    public EquipmentResponse updateEquipment(Long id, UpdateEquipmentRequest request) {
        log.info("Updating equipment id={}", id);
        Equipment equipment = findById(id);
        equipmentMapper.updateFromRequest(equipment, request);
        Equipment updated = Objects.requireNonNull(equipmentRepository.save(equipment));
        return equipmentMapper.toResponse(updated);
    }
    @Transactional
    public void deleteEquipment(Long id) {
        log.info("Deleting equipment id={}", id);
        Equipment equipment = findById(id);
        equipment.setStatus(EquipmentStatus.DECOMMISSIONED);
        equipmentRepository.save(equipment);
    }
    public AvailabilityResponse checkAvailability(Long id) {
        log.debug("Checking availability for equipmentId={}", id);
        Equipment equipment = findById(id);
        if (equipment.getStatus() == EquipmentStatus.AVAILABLE && !equipment.getMaintenanceRequired()) {
            return AvailabilityResponse.available(id);
        }
        String reason = equipment.getMaintenanceRequired()
                ? "Equipment requires maintenance before next booking"
                : "Equipment is currently " + equipment.getStatus();
        return AvailabilityResponse.unavailable(id, reason, equipment.getStatus().name());
    }
    @Transactional
    public boolean processBookingCreatedEvent(Long equipmentId) {
        log.info("Processing BookingCreatedEvent for equipmentId={}", equipmentId);
        Equipment preCheckEquipment = findById(equipmentId);
        if (preCheckEquipment.getMaintenanceRequired() || preCheckEquipment.getStatus() != EquipmentStatus.AVAILABLE) {
            log.warn("Race condition detected: equipmentId={} is not available. Rejecting booking.", equipmentId);
            return false;
        }

        int updatedRows = equipmentRepository.incrementUsageAndMarkBooked(equipmentId, maintenanceThreshold);
        if (updatedRows == 0) {
            log.error("Equipment not found for id={} — event processing failed", equipmentId);
            throw new EquipmentNotFoundException("Equipment ID " + equipmentId + " not found for event processing");
        }
        Equipment equipment = findById(equipmentId);
        if (equipment.getMaintenanceRequired()) {
            log.warn("MAINTENANCE REQUIRED triggered for equipmentId={} — usageCount={}",
                    equipmentId, equipment.getUsageCount());
        }
        log.info("Equipment {} usage updated: count={} maintenanceRequired={}",
                equipmentId, equipment.getUsageCount(), equipment.getMaintenanceRequired());
        return true;
    }
    @Transactional
    public EquipmentResponse completeMaintenance(Long id) {
        log.info("Completing maintenance for equipmentId={}", id);
        Equipment equipment = findById(id);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipment.setMaintenanceRequired(false);
        equipment.setLastMaintenanceAt(LocalDateTime.now());
        equipment.setUsageCount(0);
        Equipment updated = equipmentRepository.save(equipment);
        log.info("Maintenance completed for equipmentId={}", id);
        return equipmentMapper.toResponse(updated);
    }
    private Equipment findById(Long id) {
        return equipmentRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EquipmentNotFoundException("Equipment not found with ID: " + id));
    }
}
