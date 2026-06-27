package com.labplatform.equipment.repository;
import com.labplatform.equipment.domain.Equipment;
import com.labplatform.equipment.domain.EquipmentCategory;
import com.labplatform.equipment.domain.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByStatus(EquipmentStatus status);
    List<Equipment> findByCategory(EquipmentCategory category);
    List<Equipment> findByMaintenanceRequiredTrue();
    Optional<Equipment> findBySerialNumber(String serialNumber);
    @Query("SELECT e FROM Equipment e WHERE e.status = 'AVAILABLE' AND e.maintenanceRequired = false")
    List<Equipment> findAvailableEquipment();
    @Query("SELECT e FROM Equipment e WHERE e.category = :category AND e.status = 'AVAILABLE'")
    List<Equipment> findAvailableByCategory(@Param("category") EquipmentCategory category);
    @Modifying
    @Query("""
            UPDATE Equipment e SET
                e.usageCount = e.usageCount + 1,
                e.maintenanceRequired = CASE WHEN (e.usageCount + 1) >= :threshold THEN true ELSE e.maintenanceRequired END,
                e.status = 'BOOKED'
            WHERE e.id = :equipmentId
            """)
    int incrementUsageAndMarkBooked(
            @Param("equipmentId") Long equipmentId,
            @Param("threshold") int threshold
    );
    boolean existsBySerialNumber(String serialNumber);
}
