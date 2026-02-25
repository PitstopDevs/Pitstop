package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.*;
import com.pitstop.app.exception.UserAlreadyExistException;
import com.pitstop.app.model.WorkshopUser;
import com.pitstop.app.repository.WorkshopUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("WorkshopUserServiceImpl Unit Test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WorkshopUserServiceImplTest {

    @Autowired
    private WorkshopUserRepository workshopUserRepository;
    @Autowired
    private WorkshopUserServiceImpl workshopUserService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private WorkshopUserRegisterRequest workshopUser;

    @BeforeAll
    void setUpOnce() {
        workshopUserRepository.deleteByUsername("xxxx_xxxx_workshop_user");

        workshopUser = new WorkshopUserRegisterRequest();
        workshopUser.setName("Workshop Test Sample Name");
        workshopUser.setUsername("xxxx_xxxx_workshop_user");
        workshopUser.setEmail("xxxx_xxxx_workshop_user@xyz.com");
        workshopUser.setPassword("123456789");
    }

    @Order(1)
    @Test
    @DisplayName("WorkshopUser registering new Account")
    void saveWorkshopUserDetailsTest() {
        workshopUserService.saveWorkshopUserDetails(workshopUser);

        WorkshopUser found = workshopUserRepository.findByUsername("xxxx_xxxx_workshop_user")
                .orElseThrow(() -> new AssertionError("WorkshopUser was not saved"));

        assertEquals("Workshop Test Sample Name", found.getName());
        assertEquals("xxxx_xxxx_workshop_user", found.getUsername());
        assertEquals("xxxx_xxxx_workshop_user@xyz.com", found.getEmail());
        assertTrue(passwordEncoder.matches("123456789", found.getPassword()));
    }

    @Order(2)
    @Test
    @DisplayName("Should not register workshop user with duplicate username or email")
    void shouldNotRegisterDuplicateWorkshopUser() {
        WorkshopUserRegisterRequest duplicate = new WorkshopUserRegisterRequest();
        duplicate.setName("Duplicate Workshop");
        duplicate.setUsername("xxxx_xxxx_workshop_user");
        duplicate.setEmail("xxxx_xxxx_workshop_user@xyz.com");
        duplicate.setPassword("123456789");

        assertThrows(UserAlreadyExistException.class,
                () -> workshopUserService.saveWorkshopUserDetails(duplicate));
    }

    @Order(3)
    @DisplayName("WorkshopUser should login successfully")
    @Test
    void shouldLoginSuccessfully() {
        WorkshopLoginRequest loginRequest = new WorkshopLoginRequest();
        loginRequest.setUsername("xxxx_xxxx_workshop_user");
        loginRequest.setPassword("123456789");

        WorkshopLoginResponse response = workshopUserService.loginWorkshopUser(loginRequest);
        assertNotNull(response);
        assertEquals("xxxx_xxxx_workshop_user", response.getUsername());
        assertNotNull(response.getToken());
        assertTrue(response.getMessage().contains("Login successful"));
    }

    @Order(4)
    @DisplayName("WorkshopUser should not login with wrong credentials")
    @Test
    void shouldNotLoginWithWrongCredentials() {
        WorkshopLoginRequest loginRequest = new WorkshopLoginRequest();
        loginRequest.setUsername("xxxx_xxxx_workshop_user");
        loginRequest.setPassword("wrong");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> workshopUserService.loginWorkshopUser(loginRequest));

        assertEquals("Incorrect username or password", ex.getMessage());
    }

    @Order(5)
    @DisplayName("Adding address to workshop user's address list")
    @Test
    void addAddress() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setLatitude(22.597693666787432);
        addressRequest.setLongitude(88.35945631449115);

        workshopUserService.addAddress(addressRequest);
        SecurityContextHolder.clearContext();

        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername())
                .orElseThrow(() -> new AssertionError("WorkshopUser not found after address update"));

        assertNotNull(updated.getWorkshopAddress());
    }
    @Order(6)
    @DisplayName("Should add workshop service type")
    @Test
    void addServiceType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkshopServiceTypeRequest serviceType = new WorkshopServiceTypeRequest();
        serviceType.setWorkshopServiceType("OIL_CHANGE");
        workshopUserService.addWorkshopServiceType(serviceType);
        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername()).orElseThrow();
        assertTrue(updated.getServicesOffered().contains(WorkshopServiceType.OIL_CHANGE));
    }
    @Order(7)
    @DisplayName("Should not add duplicate service type")
    @Test
    void shouldNotAddDuplicateServiceType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkshopServiceTypeRequest serviceType = new WorkshopServiceTypeRequest();
        serviceType.setWorkshopServiceType("OIL_CHANGE");
        assertThrows(RuntimeException.class, () -> workshopUserService.addWorkshopServiceType(serviceType));
    }
    @Order(8)
    @DisplayName("Should delete workshop service type")
    @Test
    void shouldDeleteServiceType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkshopServiceTypeRequest serviceType = new WorkshopServiceTypeRequest();
        serviceType.setWorkshopServiceType("OIL_CHANGE");
        workshopUserService.deleteWorkshopServiceType(serviceType);
        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername()).orElseThrow();
        assertFalse(updated.getServicesOffered().contains(WorkshopServiceType.OIL_CHANGE));
    }
    @Order(9)
    @DisplayName("Should add workshop vehicle type")
    @Test
    void shouldAddWorkshopVehicleType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkShopVehicleTypeRequest wsVehicleTypeRequest = new WorkShopVehicleTypeRequest();
        wsVehicleTypeRequest.setWorkshopVehicleType("TWO_WHEELER");
        workshopUserService.addWorkshopVehicleType(wsVehicleTypeRequest);
        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername()).orElseThrow();
        assertEquals(VehicleType.TWO_WHEELER,updated.getVehicleTypeSupported());
    }
    @Order(10)
    @DisplayName("Should not add workshop duplicate vehicle type")
    @Test
    void shouldNotAddDuplicateVehicleType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkShopVehicleTypeRequest wsVehicleTypeRequest = new WorkShopVehicleTypeRequest();
        wsVehicleTypeRequest.setWorkshopVehicleType("TWO_WHEELER");
        assertThrows(RuntimeException.class, () -> workshopUserService.addWorkshopVehicleType(wsVehicleTypeRequest));
    }
    @Order(11)
    @DisplayName("Should delete workshop vehicle type")
    @Test
    void shouldDeleteWorkshopVehicleType() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkShopVehicleTypeRequest wsVehicleTypeRequest = new WorkShopVehicleTypeRequest();
        wsVehicleTypeRequest.setWorkshopVehicleType("TWO_WHEELER");
        workshopUserService.deleteWorkshopVehicleType();
        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername()).orElseThrow();
        assertNull(updated.getVehicleTypeSupported());
    }
    @Order(12)
    @DisplayName("Should get all workshop service types")
    @Test
    void shouldGetAllWorkshopServiceTypes() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkshopServiceTypeRequest r1 = new WorkshopServiceTypeRequest();
        r1.setWorkshopServiceType("OIL_CHANGE");
        WorkshopServiceTypeRequest r2 = new WorkshopServiceTypeRequest();
        r2.setWorkshopServiceType("TYRE_REPLACEMENT");
        workshopUserService.addWorkshopServiceType(r1);
        workshopUserService.addWorkshopServiceType(r2);
        List<WorkshopServiceType> serviceTypes = workshopUserService.getAllWorkshopServiceType();
        assertEquals(2, serviceTypes.size());
        assertTrue(serviceTypes.contains(WorkshopServiceType.OIL_CHANGE));
        assertTrue(serviceTypes.contains(WorkshopServiceType.TYRE_REPLACEMENT));
    }
    @Order(13)
    @DisplayName("Should get all workshop supported vehicle types")
    @Test
    void shouldGetAllWorkshopSupportedVehicleTypes() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        WorkShopVehicleTypeRequest workShopVehicleTypeRequest = new WorkShopVehicleTypeRequest();
        workShopVehicleTypeRequest.setWorkshopVehicleType("FOUR_WHEELER");
        workshopUserService.addWorkshopVehicleType(workShopVehicleTypeRequest);
        WorkshopUser updated = workshopUserRepository.findByUsername(workshopUser.getUsername()).orElseThrow();
        VehicleType vehicleType = updated.getVehicleTypeSupported();
        assertEquals(VehicleType.FOUR_WHEELER,vehicleType);
    }

    @AfterAll
    public void tearDownAll() {
        SecurityContextHolder.clearContext();
        workshopUserRepository.deleteByUsername("xxxx_xxxx_workshop_user");
    }
}
