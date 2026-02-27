package com.pitstop.app.service;

import com.pitstop.app.dto.*;
import com.pitstop.app.model.AppUser;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AppUserService {
    AppUserRegisterResponse saveAppUserDetails(AppUserRegisterRequest appUser);
    AppUser getAppUserById(String id);
    AppUser getAppUserByUsername(String username);
    List<AppUser> getAllAppUser();
    AddressResponse addAddress(AddressRequest address);

    String changeDefaultAddress(ChangeAddressRequest addressRequest);

    String updateAppUser(AppUserRequest appUserRequest);

    AppUserResponse getAppUserDetails();

    void changePassword(ChangePasswordRequest changePasswordRequest);

    ResponseEntity<?>  deleteAppUser();

    PersonalInfoResponse getPersonalProfile();

    GetPriceResponse getPrice(GetPriceRequest request);

    List<AddressResponse> getSavedAddress();
}
