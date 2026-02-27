package com.pitstop.app.service.impl;

import com.pitstop.app.dto.AddressResponse;
import com.pitstop.app.dto.ChangeAddressRequest;
import com.pitstop.app.dto.ChangePasswordRequest;
import com.pitstop.app.exception.BusinessException;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.model.Address;
import com.pitstop.app.model.AppUser;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.service.AppUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AppUserImplUnitTest {
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserServiceImpl appUserService;

    private void mockSecurityContext(String username) {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(authentication.getName()).thenReturn(username);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
    @Test
    void shouldChangePasswordSuccessfully() {
        String username = "john";
        mockSecurityContext(username);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword("encodedOldPassword");

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Mockito.when(passwordEncoder.matches("oldPassword", "encodedOldPassword"))
                .thenReturn(true);


        Mockito.when(passwordEncoder.matches("newPassword", "encodedOldPassword"))
                .thenReturn(false);

        Mockito.when(passwordEncoder.encode("newPassword"))
                .thenReturn("encodedNewPassword");


        appUserService.changePassword(request);

        Assertions.assertEquals("encodedNewPassword", user.getPassword());
        Mockito.verify(appUserRepository).save(user);
    }
    @Test
    void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
        String username = "john";
        mockSecurityContext(username);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword("encodedOldPassword");

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Mockito.when(passwordEncoder.matches("wrongPassword", "encodedOldPassword"))
                .thenReturn(false);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> appUserService.changePassword(request)
        );
    }
    @Test
    void shouldThrowExceptionWhenNewPasswordIsSameAsOldPassword() {
        String username = "john";
        mockSecurityContext(username);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("oldPassword");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword("encodedOldPassword");

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Mockito.when(passwordEncoder.matches("oldPassword", "encodedOldPassword"))
                .thenReturn(true);

        Mockito.when(passwordEncoder.matches("oldPassword", "encodedOldPassword"))
                .thenReturn(true);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> appUserService.changePassword(request)
        );
    }
    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String username = "john";
        mockSecurityContext(username);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> appUserService.changePassword(request)
        );
    }
    @Test
    void shouldNotSaveUserWhenCurrentPasswordIsWrong() {

        String username = "john";
        mockSecurityContext(username);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword("encodedOldPassword");

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Mockito.when(passwordEncoder.matches("wrongPassword", "encodedOldPassword"))
                .thenReturn(false);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> appUserService.changePassword(request)
        );

        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }
    @Test
    void shouldReturnSavedAddresses() {

        String username = "john";
        mockSecurityContext(username);

        Address address = new Address();
        address.setId("1L");
        address.setLatitude(12.34);
        address.setLongitude(56.78);
        address.setFormattedAddress("Mumbai, India");
        address.setDefault(true);

        List<Address> addressList = List.of(address);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(addressList);

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        List<AddressResponse> response = appUserService.getSavedAddress();

        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals("Mumbai, India", response.get(0).getFormattedAddress());
        Assertions.assertTrue(response.get(0).isDefault());
    }
    @Test
    void shouldReturnEmptyListWhenNoAddressesExist() {

        String username = "john";
        mockSecurityContext(username);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(Collections.emptyList());

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        List<AddressResponse> response = appUserService.getSavedAddress();

        Assertions.assertTrue(response.isEmpty());
    }
    @Test
    void shouldReturnEmptyListWhenAddressListIsNull() {

        String username = "john";
        mockSecurityContext(username);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(null);

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        List<AddressResponse> response = appUserService.getSavedAddress();

        Assertions.assertTrue(response.isEmpty());
    }
    @Test
    void shouldMapAddressToAddressResponseCorrectly() {

        String username = "john";
        mockSecurityContext(username);

        Address address = new Address();
        address.setId("99L");
        address.setLatitude(10.0);
        address.setLongitude(20.0);
        address.setFormattedAddress("Kolkata");
        address.setDefault(false);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(List.of(address));

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        List<AddressResponse> response = appUserService.getSavedAddress();

        AddressResponse dto = response.get(0);

        Assertions.assertEquals("99L", dto.getId());
        Assertions.assertEquals(10.0, dto.getLatitude());
        Assertions.assertEquals(20.0, dto.getLongitude());
        Assertions.assertEquals("Kolkata", dto.getFormattedAddress());
        Assertions.assertFalse(dto.isDefault());
    }
    @Test
    void shouldChangeDefaultAddressSuccessfully() {

        // Arrange
        String username = "john";
        mockSecurityContext(username);

        ChangeAddressRequest request = new ChangeAddressRequest();
        request.setId("2L");

        Address addr1 = new Address();
        addr1.setId("1L");
        addr1.setDefault(true);

        Address addr2 = new Address();
        addr2.setId("2L");
        addr2.setDefault(false);

        List<Address> addressList = List.of(addr1, addr2);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(addressList);

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        // Act
        String result = appUserService.changeDefaultAddress(request);

        // Assert
        Assertions.assertEquals("Default address updated successfully", result);

        Assertions.assertFalse(addr1.isDefault());
        Assertions.assertTrue(addr2.isDefault());

        Mockito.verify(appUserRepository).save(user);
    }
    @Test
    void shouldThrowExceptionWhenNoAddressesExist() {

        String username = "john";
        mockSecurityContext(username);

        ChangeAddressRequest request = new ChangeAddressRequest();
        request.setId("1L");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(Collections.emptyList());

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Assertions.assertThrows(
                BusinessException.class,
                () -> appUserService.changeDefaultAddress(request)
        );
    }
    @Test
    void shouldThrowExceptionWhenAddressIdNotFound() {

        String username = "john";
        mockSecurityContext(username);

        ChangeAddressRequest request = new ChangeAddressRequest();
        request.setId("99L");

        Address addr1 = new Address();
        addr1.setId("1L");

        Address addr2 = new Address();
        addr2.setId("2L");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(List.of(addr1, addr2));

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Assertions.assertThrows(
                BusinessException.class,
                () -> appUserService.changeDefaultAddress(request)
        );
    }
    @Test
    void shouldNotSaveWhenAddressNotFound() {

        String username = "john";
        mockSecurityContext(username);

        ChangeAddressRequest request = new ChangeAddressRequest();
        request.setId("99L");

        Address addr = new Address();
        addr.setId("1L");

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setUserAddress(List.of(addr));

        Mockito.when(appUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Assertions.assertThrows(
                BusinessException.class,
                () -> appUserService.changeDefaultAddress(request)
        );

        Mockito.verify(appUserRepository, Mockito.never()).save(Mockito.any());
    }
}
