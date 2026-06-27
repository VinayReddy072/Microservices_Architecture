package com.labplatform.booking.controller;
import com.labplatform.booking.dto.BookingResponse;
import com.labplatform.booking.dto.CreateBookingRequest;
import com.labplatform.booking.dto.UpdateBookingRequest;
import com.labplatform.booking.service.BookingService;
import com.labplatform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/bookings — user={}", userDetails.getUsername());
        BookingResponse booking = bookingService.createBooking(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", booking));
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        log.debug("GET /api/bookings");
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings()));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        log.debug("GET /api/bookings/{}", id);
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id)));
    }
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("GET /api/bookings/my — user={}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingsByUser(userDetails.getUsername())));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request) {
        log.info("PUT /api/bookings/{}", id);
        BookingResponse updated = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking updated successfully", updated));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        log.info("DELETE /api/bookings/{}", id);
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build(); 
    }
}
