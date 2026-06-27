package com.labplatform.booking.service;
import com.labplatform.booking.domain.Booking;
import com.labplatform.booking.domain.BookingStatus;
import com.labplatform.booking.dto.BookingResponse;
import com.labplatform.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService bookingService;
    @MockBean
    private BookingRepository bookingRepository;
    @Test
    void getBookingById_Success() {
        Booking mockBooking = Booking.builder()
                .id(1L)
                .equipmentId(10L)
                .userId("student")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        BookingResponse response = bookingService.getBookingById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("student", response.getUserId());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
    }
}
