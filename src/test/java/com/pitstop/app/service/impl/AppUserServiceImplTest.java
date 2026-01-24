package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.*;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.exception.UserAlreadyExistException;
import com.pitstop.app.model.Address;
import com.pitstop.app.model.AppUser;
import com.pitstop.app.model.PricingRule;
import com.pitstop.app.model.WorkshopUser;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.repository.PricingRuleRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import com.pitstop.app.service.AppUserService;
import com.pitstop.app.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("AppUserServiceImpl Unit Test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppUserServiceImplTest {
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AppUserServiceImpl appUserService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private AppUserRegisterRequest appUser;
    @Autowired
    private PricingRuleRepository pricingRuleRepository;
    @Autowired
    private WorkshopUserRepository workshopUserRepository;
    private WorkshopUser normalWorkshop;
    private WorkshopUser premiumWorkshop;

    @BeforeAll
    public void setUpOnce() {
        appUserRepository.deleteByUsername("xxxx_xxxx_app_user");
        workshopUserRepository.deleteByUsername("normal_ws");
        workshopUserRepository.deleteByUsername("premium_ws");
        pricingRuleRepository.deleteByVehicleTypeAndServiceType(VehicleType.TWO_WHEELER,WorkshopServiceType.OIL_CHANGE);
        appUser = new AppUserRegisterRequest();
        appUser.setName("AppUser Test Sample Name");
        appUser.setUsername("xxxx_xxxx_app_user");
        appUser.setEmail("xxxx_xxxx_app_user@xyz.com");
        appUser.setPassword("123456789");

        PricingRule rule = new PricingRule();
        rule.setVehicleType(VehicleType.TWO_WHEELER);
        rule.setServiceType(WorkshopServiceType.OIL_CHANGE);
        rule.setAmount(300);
        rule.setPremiumAmount(100);
        pricingRuleRepository.save(rule);

        normalWorkshop = new WorkshopUser();
        normalWorkshop.setUsername("normal_ws");
        normalWorkshop.setPassword("pass");
        normalWorkshop.setEmail("normal@test.com");
        normalWorkshop.setPremiumWorkshop(false);
        normalWorkshop.setVehicleTypeSupported(VehicleType.TWO_WHEELER);
        normalWorkshop.setServicesOffered(List.of(WorkshopServiceType.OIL_CHANGE));
        workshopUserRepository.save(normalWorkshop);

        premiumWorkshop = new WorkshopUser();
        premiumWorkshop.setUsername("premium_ws");
        premiumWorkshop.setPassword("pass");
        premiumWorkshop.setEmail("premium@test.com");
        premiumWorkshop.setPremiumWorkshop(true);
        premiumWorkshop.setVehicleTypeSupported(VehicleType.TWO_WHEELER);
        premiumWorkshop.setServicesOffered(List.of(WorkshopServiceType.OIL_CHANGE));
        workshopUserRepository.save(premiumWorkshop);

    }
    @Order(1)
    @Test
    @DisplayName("AppUser registering new Account")
    void saveAppUserDetailsTest(){
       appUserService.saveAppUserDetails(appUser);
       AppUser found = appUserRepository.findByUsername("xxxx_xxxx_app_user").orElseThrow(() -> new AssertionError("AppUser was not saved"));

       assertEquals("AppUser Test Sample Name",found.getName());
       assertEquals("xxxx_xxxx_app_user",found.getUsername());
       assertEquals("xxxx_xxxx_app_user@xyz.com",found.getEmail());
       assertTrue(passwordEncoder.matches("123456789", found.getPassword()));
    }
    @Order(2)
    @Test
    @DisplayName("Should not register user with duplicate username or password")
    void shouldNotRegisterNewUserWithSameCredentials() {
        AppUserRegisterRequest duplicateAppUser = new AppUserRegisterRequest();

        duplicateAppUser.setName("Duplicate");
        duplicateAppUser.setUsername("xxxx_xxxx_app_user");
        duplicateAppUser.setEmail("xxxx_xxxx_app_user@xyz.com");
        duplicateAppUser.setPassword("123456789");

        assertThrows(UserAlreadyExistException.class , () ->
                appUserService.saveAppUserDetails(duplicateAppUser));
    }
    @Order(3)
    @DisplayName("User Should login Successfully")
    @Test
    void shouldLoginSuccessfully(){
        AppUserLoginRequest appUserLoginRequest = new AppUserLoginRequest();
        appUserLoginRequest.setUsername("xxxx_xxxx_app_user");
        appUserLoginRequest.setPassword("123456789");

        AppUserLoginResponse response = appUserService.loginAppUser(appUserLoginRequest);

        assertNotNull(response);
        assertEquals("xxxx_xxxx_app_user",response.getUsername());
        assertNotNull(response.getToken());
        assertTrue(response.getMessage().contains("Login successful"));
    }
    @Order(4)
    @DisplayName("User Should Not login with wrong credentials")
    @Test
    void shouldNotLoginWithWrongCredentials(){
        AppUserLoginRequest appUserLoginRequest = new AppUserLoginRequest();
        appUserLoginRequest.setUsername("xxxx_xxxx_app_user");
        appUserLoginRequest.setPassword("wrong");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> appUserService.loginAppUser(appUserLoginRequest));

        assertEquals("Incorrect username or password", ex.getMessage());
    }
    @Order(5)
    @DisplayName("Adding address in user address List")
    @Test
    void addAddress(){
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);
            AddressRequest addressRequest = new AddressRequest();
            addressRequest.setLatitude(22.597693666787432);
            addressRequest.setLongitude(88.35945631449115);
            appUserService.addAddress(addressRequest);
            SecurityContextHolder.clearContext();

            AppUser updatedAppUser = appUserRepository.findByUsername(appUser.getUsername())
                    .orElseThrow(() -> new AssertionError("AppUser not found after address update"));
            assertNotNull(updatedAppUser.getUserAddress());
            assertFalse(updatedAppUser.getUserAddress().isEmpty());

    }
    @Test
    @Order(7)
    @DisplayName("Estimate price without workshop should return base amount only")
    void shouldReturnEstimatedPrice() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        GetPriceRequest request = new GetPriceRequest();
        request.setVehicleType("TWO_WHEELER");
        request.setServiceType("OIL_CHANGE");

        GetPriceResponse response = appUserService.getPrice(request);

        assertEquals(300, response.getBaseAmount());
        assertEquals(100, response.getPremiumAmount());
        assertEquals(300, response.getFinalAmount());
        assertFalse(response.isPremiumApplied());
    }
    @Test
    @Order(8)
    @DisplayName("Final price for normal workshop should not apply premium")
    void shouldReturnFinalPriceForNormalWorkshop() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        GetPriceRequest request = new GetPriceRequest();
        request.setVehicleType("TWO_WHEELER");
        request.setServiceType("OIL_CHANGE");
        request.setWorkshopId(normalWorkshop.getId());

        GetPriceResponse response = appUserService.getPrice(request);

        assertEquals(300, response.getBaseAmount());
        assertEquals(0, response.getPremiumAmount());
        assertEquals(300, response.getFinalAmount());
        assertFalse(response.isPremiumApplied());
    }
    @Test
    @Order(9)
    @DisplayName("Final price for premium workshop should apply premium amount")
    void shouldApplyPremiumForPremiumWorkshop() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        GetPriceRequest request = new GetPriceRequest();
        request.setVehicleType("TWO_WHEELER");
        request.setServiceType("OIL_CHANGE");
        request.setWorkshopId(premiumWorkshop.getId());

        GetPriceResponse response = appUserService.getPrice(request);

        assertEquals(300, response.getBaseAmount());
        assertEquals(100, response.getPremiumAmount());
        assertEquals(400, response.getFinalAmount());
        assertTrue(response.isPremiumApplied());
    }
    @Test
    @Order(10)
    @DisplayName("Should throw error when pricing rule does not exist")
    void shouldFailIfPricingRuleMissing() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        GetPriceRequest request = new GetPriceRequest();
        request.setVehicleType("FOUR_WHEELER");
        request.setServiceType("AC_REPAIR");

        assertThrows(RuntimeException.class, () ->
                appUserService.getPrice(request));
    }
    @Test
    @Order(11)
    @DisplayName("Should fail if workshop does not support requested service")
    void shouldFailIfWorkshopDoesNotSupportService() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        GetPriceRequest request = new GetPriceRequest();
        request.setVehicleType("TWO_WHEELER");
        request.setServiceType("ENGINE_REPAIR");
        request.setWorkshopId(normalWorkshop.getId());

        assertThrows(RuntimeException.class, () ->
                appUserService.getPrice(request));
    }
    @AfterAll
    public void tearDownAll() {
        SecurityContextHolder.clearContext();
        appUserRepository.deleteByUsername("xxxx_xxxx_app_user");
    }
}