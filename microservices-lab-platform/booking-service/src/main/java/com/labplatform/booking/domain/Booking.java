package com.labplatform.booking.domain;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_equipment_id", columnList = "equipment_id"),
        @Index(name = "idx_booking_user_id", columnList = "user_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_time_window", columnList = "start_time, end_time")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "booking_sequence", allocationSize = 1)
    private Long id;
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    public boolean overlaps(LocalDateTime startTime, LocalDateTime endTime) {
        return this.startTime.isBefore(endTime) && this.endTime.isAfter(startTime);
    }
}
