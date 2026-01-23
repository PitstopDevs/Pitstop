package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.CreatePricingRuleRequest;
import com.pitstop.app.dto.PricingRuleResponse;
import com.pitstop.app.dto.UpdatePricingRuleRequest;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.model.PricingRule;
import com.pitstop.app.repository.AdminUserRepository;
import com.pitstop.app.repository.PricingRuleRepository;
import com.pitstop.app.service.AdminPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPricingServiceImpl implements AdminPricingService {
    private final PricingRuleRepository pricingRuleRepository;
    @Override
    public PricingRuleResponse createPricingRule(CreatePricingRuleRequest request) {
        try {
            log.info("Creating rule vehicleType={}, serviceType={}",
                    request.getVehicleType(), request.getWorkshopServiceType());
            VehicleType vt = VehicleType.valueOf(request.getVehicleType().toUpperCase());
            WorkshopServiceType st = WorkshopServiceType.valueOf(request.getWorkshopServiceType().toUpperCase());
            pricingRuleRepository.findByVehicleTypeAndServiceType(vt,st)
                    .ifPresent(pricingRule -> {
                        log.warn("Pricing rule already exists for {} and {}",request.getVehicleType(), request.getWorkshopServiceType());
                        throw new RuntimeException("Pricing rule already exists for " + request.getVehicleType() + " and " + request.getWorkshopServiceType());
                    });
            PricingRule pricingRule = PricingRule.builder()
                    .vehicleType(vt)
                    .serviceType(st)
                    .amount(request.getAmount())
                    .premiumAmount(request.getPremiumAmount())
                    .createdDate(LocalDateTime.now())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();
            pricingRuleRepository.save(pricingRule);

            return PricingRuleResponse.builder()
                    .id(pricingRule.getId())
                    .vehicleType(pricingRule.getVehicleType())
                    .workshopServiceType(pricingRule.getServiceType())
                    .amount(pricingRule.getAmount())
                    .premiumAmount(pricingRule.getPremiumAmount())
                    .createdDate(pricingRule.getCreatedDate())
                    .updatedDate(pricingRule.getLastModifiedDate())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create rule: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public PricingRuleResponse updatePricingRule(UpdatePricingRuleRequest request, String id) {
        log.info("Updating pricing rule id {}",id);
        try {
            PricingRule pricingRule = pricingRuleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pricing rule id " + id + " not found"));

            if(request.getAmount() == pricingRule.getAmount()) {
                log.warn("Pricing rule cannot be updated as amount is same");
                throw new RuntimeException("Pricing rule cannot be updated as amount is same");
            }
            if(request.getPremiumAmount() == pricingRule.getPremiumAmount()) {
                log.warn("Pricing rule cannot be updated as premium amount is same");
                throw new RuntimeException("Pricing rule cannot be updated as premium amount is same");
            }
            if(request.getAmount() != 0.0){
                log.info("Updating amount from {} to {}",pricingRule.getAmount(),request.getAmount());
                pricingRule.setAmount(request.getAmount());
            }
            if (request.getPremiumAmount() != 0.0){
                log.info("Updating premium account from {} to {}",pricingRule.getPremiumAmount(),request.getPremiumAmount());
                pricingRule.setPremiumAmount(request.getPremiumAmount());
            }
            pricingRule.setLastModifiedDate(LocalDateTime.now());
            pricingRuleRepository.save(pricingRule);

            return PricingRuleResponse.builder()
                    .id(pricingRule.getId())
                    .vehicleType(pricingRule.getVehicleType())
                    .workshopServiceType(pricingRule.getServiceType())
                    .amount(request.getAmount())
                    .premiumAmount(request.getPremiumAmount())
                    .createdDate(pricingRule.getCreatedDate())
                    .updatedDate(pricingRule.getLastModifiedDate())
                    .build();

        } catch (Exception e) {
            log.error("Failed to update pricing rule: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update pricing rule: " + e.getMessage());
        }
    }

    @Override
    public void deletePricingRule(String id) {
        try{
            log.info("Deleting pricing rule id {}",id);
            PricingRule pricingRule = pricingRuleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pricing rule id " + id + " not found"));
            pricingRuleRepository.delete(pricingRule);
        }
        catch (Exception e){
            log.error("Failed to delete pricing rule: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete pricing rule: " + e.getMessage());
        }
    }

    @Override
    public List<PricingRuleResponse> getAllPricingRules() {
        log.info("Getting all pricing rules");
        try{
            List<PricingRule> pricingRules = pricingRuleRepository.findAll();
            if (pricingRules.isEmpty()) {
                log.warn("No pricing rules found");
                return Collections.emptyList();
            }

            return pricingRules.stream()
                    .map(rule -> PricingRuleResponse.builder()
                            .id(rule.getId())
                            .vehicleType(rule.getVehicleType())
                            .workshopServiceType(rule.getServiceType())
                            .amount(rule.getAmount())
                            .premiumAmount(rule.getPremiumAmount())
                            .createdDate(rule.getCreatedDate())
                            .build()).toList();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to find all pricing rules", e);
        }
    }

    @Override
    public PricingRuleResponse getPricingRuleByVehicleTypeAndServiceType(VehicleType vehicleType, WorkshopServiceType serviceType) {
        log.info("Getting pricing rule for {} and {}", vehicleType.toString(), serviceType.toString());
        try {
            // Scanning for the specific vehicleType & serviceType Combination
            PricingRule pricingRule = pricingRuleRepository.findByVehicleTypeAndServiceType(vehicleType, serviceType)
                    .orElseThrow(() -> new RuntimeException("Pricing rule not found"));

            // Returning the Pricing Rule
            return PricingRuleResponse.builder()
                    .id(pricingRule.getId())
                    .vehicleType(pricingRule.getVehicleType())
                    .workshopServiceType(pricingRule.getServiceType())
                    .amount(pricingRule.getAmount())
                    .premiumAmount(pricingRule.getPremiumAmount())
                    .createdDate(pricingRule.getCreatedDate())
                    .updatedDate(pricingRule.getLastModifiedDate())
                    .build();
        }
        catch (Exception e) {
            // Specific vehicleType & serviceType Combination not found
            log.warn("No Pricing Rule found for requested combination of Vehicle Type :: {} and Service Type :: {}", vehicleType.toString(), serviceType.toString());
            log.warn("Returning default pricing rule values for Vehicle Type :: {} and Service Type :: {}", vehicleType.toString(), serviceType.toString());

            return new PricingRuleResponse("default", vehicleType, serviceType, 499.00, 199.00, LocalDateTime.now(), LocalDateTime.now());
        }
    }
}
