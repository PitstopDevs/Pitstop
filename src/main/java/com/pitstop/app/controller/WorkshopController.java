package com.pitstop.app.controller;

import com.pitstop.app.dto.*;
import com.pitstop.app.service.impl.BookingHistoryServiceImpl;
import com.pitstop.app.service.impl.WorkshopUserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopUserServiceImpl workshopService;
    private final BookingHistoryServiceImpl bookingHistoryService;

    // Role should be NORMAL_WORKSHOP_USER
    // Remove {workshopUserId} part from path variable after auth is implemented
    // Directly pull workshopUser details from Request Object
    @PostMapping("/setWorkshopStatus")
    public ResponseEntity<WorkshopStatusResponse> openWorkshop() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopStatusResponse response = workshopService.openWorkshop(username);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/getCurrentStatus")
    public ResponseEntity<WorkshopStatusResponse> getCurrentStatus(){
        return ResponseEntity.ok(workshopService.getWorkshopCurrentStatus());
    }
    @PostMapping("/setCloseWorkshop")
    public ResponseEntity<WorkshopStatusResponse> closeWorkshop() {
        return ResponseEntity.ok(workshopService.closeWorkshop());
    }

     /*
    Create Secured endpoints / API for the below functionality:
    1. (PUT) Can update address
    2. (GET) Fetch bookingHistory - Create relevant DTO to return
    3. (PUT) Update email
    4. (PUT) Update password
    5. (DELETE) Delete account
     */
     @PutMapping("/update-address")
     public ResponseEntity<AddressResponse> addAddress(@RequestBody AddressRequest address){
         return new ResponseEntity<>(workshopService.addAddress(address),HttpStatus.OK);
     }
     @GetMapping("/getAddress")
     public ResponseEntity<WorkshopAddressResponse> getAddress(){
         return new ResponseEntity<>(workshopService.getAddress(),HttpStatus.OK);
     }

    @GetMapping("/getBookingHistory")
    public ResponseEntity<?> getBookingHistoryForWorkShopUser() {
        return new ResponseEntity<>(bookingHistoryService.getBookingHistoryForWorkShopUser(), HttpStatus.OK);
    }

     @GetMapping("/me")
     public ResponseEntity<WorkshopUserResponse> getCurrentWorkshopUser(){
         return new ResponseEntity<>(workshopService.getWorkshopUserDetails(),HttpStatus.OK);
     }
     @PutMapping("/me/update")
    public ResponseEntity<String> updateWorkshopUser(@RequestBody WorkshopUserRequest workshopUserRequest){
         return new ResponseEntity<>(workshopService.updateWorkshopUser(workshopUserRequest),HttpStatus.OK);
     }
    @PutMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        workshopService.changePassword(changePasswordRequest);
        return ResponseEntity.ok(
                Map.of("message","Password changed successfully")
        );
    }
    @DeleteMapping("/me/delete")
    public ResponseEntity<?> deleteAppUserDetails() {
        return new ResponseEntity<>(workshopService.deleteAppUser(),HttpStatus.OK);
    }
    @GetMapping("/profile")
    public ResponseEntity<PersonalInfoResponse> getPersonalInfo() {
        return new ResponseEntity<>(workshopService.getPersonalProfile(),HttpStatus.OK);
    }
    @PutMapping("/addWorkshopService")
    public ResponseEntity<?> addWorkshopService(@RequestBody WorkshopServiceTypeRequest workshopServiceType) {
         try{
             workshopService.addWorkshopServiceType(workshopServiceType);
             return ResponseEntity.ok("WORKSHOP SERVICE TYPE ADDED SUCCESSFULLY");
         } catch (Exception e) {
             return new ResponseEntity<>("FAILED TO ADD WORKSHOP SERVICE", HttpStatus.INTERNAL_SERVER_ERROR);
         }
    }
    @PutMapping("/addWorkshopVehicleType")
    public ResponseEntity<?> addWorkShopVehicleType(@RequestBody WorkShopVehicleTypeRequest workshopVehicleTypeRequest) {
        try{
            workshopService.addWorkshopVehicleType(workshopVehicleTypeRequest);
            return ResponseEntity.ok("WORKSHOP VEHICLE TYPE ADDED SUCCESSFULLY");
        } catch (Exception e) {
            return new ResponseEntity<>("FAILED TO ADD WORKSHOP SERVICE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/removeWorkshopService")
    public ResponseEntity<?> removeWorkshopService(@RequestBody WorkshopServiceTypeRequest workshopServiceType) {
        try{
            workshopService.deleteWorkshopServiceType(workshopServiceType);
            return ResponseEntity.ok("WORKSHOP SERVICE TYPE REMOVED SUCCESSFULLY");
        }
        catch (Exception e) {
            throw new RuntimeException("FAILED TO REMOVE WORKSHOP SERVICE" +HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/removeVehicle")
    public ResponseEntity<?> removeWorkshopVehicleType(@RequestBody WorkShopVehicleTypeRequest workshopVehicleTypeRequest) {
        try{
            workshopService.deleteWorkshopVehicleType(workshopVehicleTypeRequest);
            return ResponseEntity.ok("WORKSHOP VEHICLE TYPE REMOVED SUCCESSFULLY");
        }
        catch (Exception e) {
            throw new RuntimeException("FAILED TO REMOVE WORKSHOP VEHICLE TYPE" +HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/me/services")
    public ResponseEntity<?> getWorkshopServices(){
         try{
            return new ResponseEntity<>(workshopService.getAllWorkshopServiceType(),HttpStatus.OK);
         } catch (Exception e) {
             throw new RuntimeException("FAILED TO GET WORKSHOP SERVICE TYPES" +HttpStatus.INTERNAL_SERVER_ERROR);
         }
    }
    @GetMapping("/me/vehicleTypes")
    public ResponseEntity<?> getWorkshopSupportedVehicleTypes(){
        try{
            return new ResponseEntity<>(workshopService.getWorkshopSupportedVehicleType(),HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("FAILED TO GET WORKSHOP SERVICE TYPES" +HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
