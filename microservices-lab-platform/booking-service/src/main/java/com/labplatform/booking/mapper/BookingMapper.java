package com.labplatform.booking.mapper;
import com.labplatform.booking.domain.Booking;
import com.labplatform.booking.dto.BookingResponse;
import com.labplatform.booking.dto.CreateBookingRequest;
import org.mapstruct.*;
import java.util.List;
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BookingMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toEntity(CreateBookingRequest request);
    BookingResponse toResponse(Booking booking);
    List<BookingResponse> toResponseList(List<Booking> bookings);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "equipmentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(
            @MappingTarget Booking booking,
            com.labplatform.booking.dto.UpdateBookingRequest request
    );
}
