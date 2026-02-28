package com.pitstop.app.repository;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.model.PricingRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends MongoRepository<PricingRule, String> {
    Optional<PricingRule> findByVehicleTypeAndServiceType(VehicleType vehicleType, WorkshopServiceType serviceType);

    void deleteByVehicleTypeAndServiceType(VehicleType vehicleType, WorkshopServiceType serviceType);

    List<PricingRule> findByVehicleTypeIn(List<VehicleType> vehicleTypes);
}
