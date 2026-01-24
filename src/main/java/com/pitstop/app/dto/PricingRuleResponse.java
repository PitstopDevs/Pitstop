package com.pitstop.app.dto;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
public class PricingRuleResponse {
    private String id;
    private VehicleType vehicleType;
    private WorkshopServiceType workshopServiceType;
    private double amount;
    private double premiumAmount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
