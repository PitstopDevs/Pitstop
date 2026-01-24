package com.pitstop.app.service;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.CreatePricingRuleRequest;
import com.pitstop.app.dto.PricingRuleResponse;
import com.pitstop.app.dto.UpdatePricingRuleRequest;

import java.util.List;

public interface AdminPricingService {
    PricingRuleResponse createPricingRule(CreatePricingRuleRequest request);
    PricingRuleResponse updatePricingRule(UpdatePricingRuleRequest request, String id);
    void deletePricingRule(String id);
    List<PricingRuleResponse> getAllPricingRules();
    PricingRuleResponse getPricingRuleByVehicleTypeAndServiceType(VehicleType vehicleType, WorkshopServiceType serviceType);
}
