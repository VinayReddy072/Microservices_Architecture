package com.labplatform.booking.repository;
import com.labplatform.booking.domain.Booking;
import com.labplatform.booking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
            SELECT b FROM Booking b
            WHERE b.equipmentId = :equipmentId
            AND b.status NOT IN ('CANCELLED', 'COMPLETED')
            AND b.startTime < :endTime
            AND b.endTime > :startTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByEquipmentIdOrderByStartTimeAsc(Long equipmentId);
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    @Query("""
            SELECT b FROM Booking b
            WHERE b.startTime >= :from AND b.endTime <= :to
            ORDER BY b.startTime ASC
            """)
    List<Booking> findBookingsInDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.equipmentId = :equipmentId
            AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')
            """)
    long countActiveBookingsByEquipment(@Param("equipmentId") Long equipmentId);
}
