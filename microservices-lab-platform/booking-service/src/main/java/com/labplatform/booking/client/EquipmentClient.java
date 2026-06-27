package com.labplatform.booking.client;
import com.labplatform.booking.client.dto.AvailabilityResponse;
import com.labplatform.booking.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(
    name = "equipment-service",
    configuration = FeignConfig.class,
    fallback = EquipmentClientFallback.class
)
public interface EquipmentClient {
    @GetMapping("/api/equipment/{id}/availability")
    AvailabilityResponse checkAvailability(@PathVariable("id") Long equipmentId);
}
