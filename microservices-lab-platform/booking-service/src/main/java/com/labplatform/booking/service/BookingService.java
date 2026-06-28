package com.labplatform.booking.service;
import com.labplatform.booking.client.EquipmentClient;
import com.labplatform.booking.client.dto.AvailabilityResponse;
import com.labplatform.booking.domain.Booking;
import com.labplatform.booking.domain.BookingStatus;
import com.labplatform.booking.dto.BookingResponse;
import com.labplatform.booking.dto.CreateBookingRequest;
import com.labplatform.booking.dto.UpdateBookingRequest;
import com.labplatform.booking.exception.BookingConflictException;
import com.labplatform.booking.exception.BookingNotFoundException;
import com.labplatform.booking.exception.EquipmentUnavailableException;
import com.labplatform.booking.mapper.BookingMapper;
import com.labplatform.booking.messaging.BookingEventPublisher;
import com.labplatform.booking.repository.BookingRepository;
import com.labplatform.common.event.BookingCreatedEvent;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final EquipmentClient equipmentClient;
    private final BookingEventPublisher eventPublisher;
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String username) {
        log.info("Creating booking for equipmentId={} by userId={}", request.getEquipmentId(), username);
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        log.debug("Checking equipment availability for equipmentId={}", request.getEquipmentId());
        AvailabilityResponse availability = checkEquipmentAvailability(request.getEquipmentId());
        if (!availability.isAvailable()) {
            log.warn("Equipment {} is not available for booking", request.getEquipmentId());
            throw new EquipmentUnavailableException(
                    "Equipment ID " + request.getEquipmentId() + " is currently not available. " +
                    "Reason: " + (availability.getReason() != null ? availability.getReason() : "Equipment is BOOKED or under MAINTENANCE")
            );
        }
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                request.getEquipmentId(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            log.warn("Booking conflict detected for equipmentId={} — {} conflicting booking(s)",
                    request.getEquipmentId(), conflicts.size());
            throw new BookingConflictException(
                    "Equipment ID " + request.getEquipmentId() +
                    " already has " + conflicts.size() + " booking(s) in the requested time window."
            );
        }
        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(username);
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully: bookingId={}", savedBooking.getId());
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(savedBooking.getId())
                .equipmentId(savedBooking.getEquipmentId())
                .userId(savedBooking.getUserId())
                .startTime(savedBooking.getStartTime())
                .endTime(savedBooking.getEndTime())
                .build();
        eventPublisher.publishBookingCreated(event);
        return bookingMapper.toResponse(savedBooking);
    }
    @Retry(name = "equipment-service")
    public AvailabilityResponse checkEquipmentAvailability(Long equipmentId) {
        return equipmentClient.checkAvailability(equipmentId);
    }
    public List<BookingResponse> getAllBookings() {
        log.debug("Fetching all bookings");
        return bookingMapper.toResponseList(bookingRepository.findAll());
    }
    public BookingResponse getBookingById(Long id) {
        log.debug("Fetching booking by id={}", id);
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
        return bookingMapper.toResponse(booking);
    }
    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {
        log.info("Updating booking id={}", id);
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
        LocalDateTime newStart = request.getStartTime() != null ? request.getStartTime() : booking.getStartTime();
        LocalDateTime newEnd = request.getEndTime() != null ? request.getEndTime() : booking.getEndTime();
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        bookingMapper.updateEntityFromRequest(booking, request);
        Booking updated = bookingRepository.save(booking);
        log.info("Booking updated: bookingId={}", updated.getId());
        return bookingMapper.toResponse(updated);
    }
    @Transactional
    public void cancelBooking(Long id) {
        log.info("Cancelling booking id={}", id);
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking cancelled: bookingId={}", id);
    }
    public List<BookingResponse> getBookingsByUser(String username) {
        log.debug("Fetching bookings for userId={}", username);
        return bookingMapper.toResponseList(
                bookingRepository.findByUserIdOrderByCreatedAtDesc(username));
    }
}
