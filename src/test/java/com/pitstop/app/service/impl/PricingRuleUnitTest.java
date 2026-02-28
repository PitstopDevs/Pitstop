package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.model.PricingRule;
import com.pitstop.app.repository.PricingRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PricingRuleUnitTest {
    @Mock
    private PricingRuleRepository pricingRuleRepository;
    @InjectMocks
    private WorkshopSearchServiceImpl workshopSearchService;

    @Test
    @DisplayName("Should Return Two & Both Services When Vehicle Type is TWO")
    void shouldReturnTwoAndBothServices_whenVehicleTypeIsTWO() {

        // Arrange
        PricingRule bothRule = new PricingRule();
        bothRule.setVehicleType(VehicleType.BOTH);
        bothRule.setServiceType(WorkshopServiceType.OIL_CHANGE);

        PricingRule twoRule = new PricingRule();
        twoRule.setVehicleType(VehicleType.TWO_WHEELER);
        twoRule.setServiceType(WorkshopServiceType.TYRE_REPLACEMENT);

        List<PricingRule> mockRules = List.of(bothRule, twoRule);

        when(pricingRuleRepository.findByVehicleTypeIn(
                List.of(VehicleType.BOTH, VehicleType.TWO_WHEELER)
        )).thenReturn(mockRules);

        // Act
        List<WorkshopServiceType> result =
                workshopSearchService.getAvailableServices(VehicleType.TWO_WHEELER);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(WorkshopServiceType.OIL_CHANGE));
        assertTrue(result.contains(WorkshopServiceType.TYRE_REPLACEMENT));
    }
    @Test
    @DisplayName("Should Return Four & Both Services When Vehicle Type is FOUR")
    void shouldReturnFourAndBothServices_whenVehicleTypeIsFOUR() {

        PricingRule bothRule = new PricingRule();
        bothRule.setVehicleType(VehicleType.BOTH);
        bothRule.setServiceType(WorkshopServiceType.OIL_CHANGE);

        PricingRule fourRule = new PricingRule();
        fourRule.setVehicleType(VehicleType.FOUR_WHEELER);
        fourRule.setServiceType(WorkshopServiceType.AC_REPAIR);

        List<PricingRule> mockRules = List.of(bothRule, fourRule);

        when(pricingRuleRepository.findByVehicleTypeIn(
                List.of(VehicleType.BOTH, VehicleType.FOUR_WHEELER)
        )).thenReturn(mockRules);

        List<WorkshopServiceType> result =
                workshopSearchService.getAvailableServices(VehicleType.FOUR_WHEELER);

        assertEquals(2, result.size());
        assertTrue(result.contains(WorkshopServiceType.OIL_CHANGE));
        assertTrue(result.contains(WorkshopServiceType.AC_REPAIR));
    }
    @Test
    @DisplayName("Should Return Distinct Services when Duplicate Rule Exist")
    void shouldReturnDistinctServices_whenDuplicateRulesExist() {

        PricingRule bothRule1 = new PricingRule();
        bothRule1.setVehicleType(VehicleType.BOTH);
        bothRule1.setServiceType(WorkshopServiceType.OIL_CHANGE);

        PricingRule bothRule2 = new PricingRule();
        bothRule2.setVehicleType(VehicleType.BOTH);
        bothRule2.setServiceType(WorkshopServiceType.OIL_CHANGE);

        when(pricingRuleRepository.findByVehicleTypeIn(anyList()))
                .thenReturn(List.of(bothRule1, bothRule2));

        List<WorkshopServiceType> result =
                workshopSearchService.getAvailableServices(VehicleType.TWO_WHEELER);

        assertEquals(1, result.size());
    }
}
