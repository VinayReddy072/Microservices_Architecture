package com.labplatform.booking.client;
import com.labplatform.booking.client.dto.AvailabilityResponse;
import com.labplatform.booking.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class EquipmentClientFallback implements EquipmentClient {
    @Override
    public AvailabilityResponse checkAvailability(Long equipmentId) {
        log.error("Equipment service unavailable — circuit breaker fallback triggered for equipmentId={}", equipmentId);
        throw new ServiceUnavailableException(
                "Equipment service is temporarily unavailable. " +
                "Cannot verify equipment availability. Please try again in a few moments."
        );
    }
}
