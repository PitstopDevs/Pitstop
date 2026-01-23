package com.pitstop.app.controller;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.dto.*;
import com.pitstop.app.service.impl.BookingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingServiceImpl bookingService;

    // Role should be APP_USER
    @GetMapping("/openWorkshops")
    public ResponseEntity<List<WorkshopStatusResponse>> getAllOpenWorkshops() {
        return new ResponseEntity<>(bookingService.getAllOpenWorkshops(), HttpStatus.OK);
    }

    // Role should be APP_USER
    @PostMapping
    public ResponseEntity<?> requestBooking(@RequestBody RequestBooking requestBooking) {
        String bookingId = null;
        try {
            bookingId = bookingService.requestBooking(requestBooking.getWorkShopUserId(),
                    requestBooking.getServiceType(), requestBooking.getVehicleId());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(bookingId, HttpStatus.OK);
    }

    // Role should be APP_USER
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> checkBookingStatusForAppUser(@PathVariable String bookingId) {
        try {
            return new ResponseEntity<>(bookingService.checkBookingStatus(bookingId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Role should be WORKSHOP_USER
    @GetMapping("/check")
    public ResponseEntity<List<BookingResponse>> checkQueueOfBookingsForWorkshopUser() {
        return new ResponseEntity<>(bookingService.getStartedBookings(), HttpStatus.OK);
    }

    // Role should be WORKSHOP_USER
    @GetMapping("/acceptBooking/{bookingId}")
    public ResponseEntity<?> acceptBooking(@PathVariable String bookingId) {
        try {
            return new ResponseEntity<>(bookingService.acceptBooking(bookingId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @GetMapping("/rejectBooking/{bookingId}")
    public ResponseEntity<?> rejectBooking(@PathVariable String bookingId) {
        try {
            return new ResponseEntity<>(bookingService.rejectBooking(bookingId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be APP_USER
    @GetMapping("/startJourney/{bookingId}")
    public ResponseEntity<?> startJourney(@PathVariable String bookingId) {
        try {
            return new ResponseEntity<>(bookingService.startJourney(bookingId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be APP_USER
    @PostMapping("/generateOtp/{bookingId}")
    public ResponseEntity<?> generateOtp(@PathVariable String bookingId) {
        try {
            return new ResponseEntity<>(bookingService.generateBookingOtp(bookingId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @PostMapping("/verifyOtpAndSetWaiting")
    public ResponseEntity<?> verifyOtpAndSetWaiting(@RequestBody BookingRequestOtp bookingRequestOtp) {
        try {
            bookingService.verifyOtpAndSetStatus(bookingRequestOtp, BookingStatus.WAITING);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @PostMapping("/verifyOtpAndSetRepairing")
    public ResponseEntity<?> verifyOtpAndSetRepairing(@RequestBody BookingRequestOtp bookingRequestOtp) {
        try {
            bookingService.verifyOtpAndSetStatus(bookingRequestOtp, BookingStatus.REPAIRING);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @PostMapping("/verifyOtpAndSetCompleted")
    public ResponseEntity<?> verifyOtpAndSetCompleted(@RequestBody BookingRequestOtp bookingRequestOtp) {
        try {
            bookingService.verifyOtpAndSetStatus(bookingRequestOtp, BookingStatus.COMPLETED);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be APP_USER
    @PostMapping("/cancelBooking/{bookingId}")
    public ResponseEntity<?> cancelByAppUser(@PathVariable String bookingId) {
        try {
            bookingService.cancelBookingByAppUser(bookingId);
            return new ResponseEntity<>("Booking Successfully Cancelled!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @PostMapping("/cancelBooking")
    public ResponseEntity<?> cancelByWorkShopUser(@RequestBody BookingRequestOtp bookingRequestOtp) {
        try {
            bookingService.cancelBookingByWorkshopUser(bookingRequestOtp);
            return new ResponseEntity<>("Booking Successfully Cancelled!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be WORKSHOP_USER
    @PostMapping("/giveRatingToAppUser")
    public ResponseEntity<?> giveRatingToAppUser(@RequestBody AppUserRatingRequest appUserRatingRequest) {
        try {
            bookingService.giveRatingToAppUser(appUserRatingRequest);
            return new ResponseEntity<>("Rating Successfully added for AppUser!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Role should be APP_USER
    @PostMapping("/giveRatingToWorkShopUser")
    public ResponseEntity<?> giveRatingToWorkShopUser(@RequestBody WorkShopUserRatingRequest workShopUserRatingRequest) {
        try {
            bookingService.giveRatingToWorkShopUser(workShopUserRatingRequest);
            return new ResponseEntity<>("Rating Successfully added for WorkShopUser!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
