package com.labplatform.equipment.domain;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
@Entity
@Table(
    name = "equipment",
    indexes = {
        @Index(name = "idx_equipment_status", columnList = "status"),
        @Index(name = "idx_equipment_category", columnList = "category"),
        @Index(name = "idx_equipment_maintenance", columnList = "maintenance_required")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_seq")
    @SequenceGenerator(name = "equipment_seq", sequenceName = "equipment_sequence", allocationSize = 1)
    private Long id;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private EquipmentCategory category;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;
    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;
    @Column(name = "location", length = 200)
    private String location;
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;
    @Column(name = "maintenance_required", nullable = false)
    @Builder.Default
    private Boolean maintenanceRequired = false;
    @Column(name = "last_maintenance_at")
    private LocalDateTime lastMaintenanceAt;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    public boolean isBookable() {
        return status == EquipmentStatus.AVAILABLE && !maintenanceRequired;
    }
    public void incrementUsage(int maintenanceThreshold) {
        this.usageCount++;
        if (this.usageCount >= maintenanceThreshold) {
            this.maintenanceRequired = true;
        }
    }
}
