package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.model.Vehicle;
import com.pitstop.app.repository.VehicleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class VehicleServiceImplTest {

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Mock
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    Vehicle activa6G =  new Vehicle(VehicleType.TWO_WHEELER, "Honda", "Activa 6G", 110);

    @Test
    void saveVehicleTestTwoWheeler() {
        when(vehicleRepository.save(activa6G)).thenReturn(Vehicle.builder().vehicleType(VehicleType.TWO_WHEELER).brand("Honda").model("Activa 6G").engineCapacity(110).build());
        Vehicle v = vehicleService.saveVehicle(activa6G);
        Assertions.assertEquals(VehicleType.TWO_WHEELER, v.getVehicleType());
    }
}
