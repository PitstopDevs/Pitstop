package com.pitstop.app.controller;

import com.pitstop.app.dto.*;
import com.pitstop.app.model.Address;
import com.pitstop.app.service.VehicleService;
import com.pitstop.app.service.impl.AppUserServiceImpl;
import com.pitstop.app.service.impl.BookingHistoryServiceImpl;
import com.pitstop.app.service.impl.VehicleServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserServiceImpl appUserService;
    private final BookingHistoryServiceImpl bookingHistoryService;
    private final VehicleServiceImpl vehicleService;

    /*
    Create Secured endpoints / API for the below functionality:
    1. (PUT) Can add address (Add in the userAddress List)
    2. (PUT) Can add balance to wallet (Skip for now as payment will be involved)
    3. (GET) Fetch bookingHistory - Create relevant DTO to return
    4. (PUT) Update email
    5. (PUT) Update password
    6. (DELETE) Delete account
     */

    @PostMapping("/add-address")
    public ResponseEntity<AddressResponse> addAddress(@RequestBody AddressRequest address) {
        AddressResponse response = appUserService.addAddress(address);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/savedAddress")
    public ResponseEntity<List<AddressResponse>> getSavedAddress(){
        List<AddressResponse> responses = appUserService.getSavedAddress();
        return new ResponseEntity<>(responses,HttpStatus.OK);
    }
    @PutMapping("/change-default-address")
    public ResponseEntity<String> changeDefaultAddress(@RequestBody ChangeAddressRequest addressRequest) {
        return new ResponseEntity<>(appUserService.changeDefaultAddress(addressRequest),HttpStatus.OK);
    }

    @GetMapping("/getBookingHistory")
    public ResponseEntity<?> getBookingHistoryForAppUser() {
        return new ResponseEntity<>(bookingHistoryService.getBookingHistoryForAppUser(), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<AppUserResponse> getCurrentUserDetails(){
        return new ResponseEntity<>(appUserService.getAppUserDetails(),HttpStatus.OK);
    }
    @PutMapping("/me/update")
    public ResponseEntity<String> updateUserDetails(@RequestBody AppUserRequest appUserRequest) {
        return new ResponseEntity<>(appUserService.updateAppUser(appUserRequest),HttpStatus.OK);
    }
    @PutMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        appUserService.changePassword(changePasswordRequest);
        return ResponseEntity.ok(
                Map.of("message","Password changed successfully")
        );
    }
    @DeleteMapping("/me/delete")
    public ResponseEntity<?> deleteAppUserDetails() {
        return new ResponseEntity<>(appUserService.deleteAppUser(),HttpStatus.OK);
    }
    @GetMapping("/profile")
    public ResponseEntity<PersonalInfoResponse> getPersonalInfo() {
        return new ResponseEntity<>(appUserService.getPersonalProfile(),HttpStatus.OK);
    }
    @PostMapping("/addTwoWheeler")
    public ResponseEntity<AddVehicleResponse> addTwoWheeler(@RequestBody AddVehicleRequest addVehicleRequest) {
        try {
            return new ResponseEntity<>(vehicleService.addTwoWheeler(addVehicleRequest), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new AddVehicleResponse(null, "FAILED_TO_ADD_VEHICLE"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/addFourWheeler")
    public ResponseEntity<AddVehicleResponse> addFourWheeler(@RequestBody AddVehicleRequest addVehicleRequest) {
        try {
            return new ResponseEntity<>(vehicleService.addFourWheeler(addVehicleRequest), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new AddVehicleResponse(null, "FAILED_TO_ADD_VEHICLE"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getAllVehicles")
    public ResponseEntity<List<VehicleDetailsResponse>> getAllVehicles() {
        try {
            return new ResponseEntity<>(vehicleService.getAllVehicles(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getVehicle/{vehicleId}")
    public ResponseEntity<VehicleDetailsResponse> getVehicleById(@PathVariable String vehicleId) {
        try {
            VehicleDetailsResponse vehicleDetailsResponse = vehicleService.getVehicleById(vehicleId);
            if(vehicleDetailsResponse != null)
                return new ResponseEntity<>(vehicleDetailsResponse, HttpStatus.OK);
            else
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/removeVehicle/{vehicleId}")
    public ResponseEntity<HttpStatus> deleteVehicleById(@PathVariable String vehicleId) {
        try {
            vehicleService.deleteVehicle(vehicleId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("/getPrice")
    public ResponseEntity<GetPriceResponse> getPrice(@RequestBody GetPriceRequest getPriceRequest) {
        try{
            return new ResponseEntity<>(appUserService.getPrice(getPriceRequest), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
