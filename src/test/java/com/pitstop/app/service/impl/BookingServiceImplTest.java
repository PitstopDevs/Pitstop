package com.pitstop.app.service.impl;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.*;
import com.pitstop.app.model.AppUser;
import com.pitstop.app.model.Booking;
import com.pitstop.app.model.WorkshopUser;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.repository.BookingRepository;
import com.pitstop.app.repository.PaymentRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class BookingServiceImplTest {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private AppUserServiceImpl appUserService;

    @Autowired
    private WorkshopUserServiceImpl workshopUserService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WorkshopUserRepository workshopUserRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentServiceImpl paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private VehicleServiceImpl vehicleService;

    private AppUser appUser;
    private WorkshopUser workshopUser;
    private String bookingId;
    private InitiatePaymentResponse initiatePaymentResponse = null;
    private String vehicleId;

    @BeforeAll
    public void setUpOnce() {
        workshopUserRepository.deleteByUsername("xxxx_xxxx_workshop_user");
        appUserRepository.deleteByUsername("test_app_user_001");
        // AppUser Set-Up
        appUser = new AppUser();
        appUser.setName("Test App User");
        appUser.setUsername("test_app_user_001");
        appUser.setEmail("test_app_user_001@example.com");
        appUser.setPassword("test@123");
        appUserRepository.save(appUser);


        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);
            AddressRequest addressRequest = new AddressRequest();
            addressRequest.setLatitude(22.597693666787432);
            addressRequest.setLongitude(88.35945631449115);
            appUserService.addAddress(addressRequest);
            SecurityContextHolder.clearContext();
        }

        // WorkshopUser Set-Up
        workshopUser = new WorkshopUser();
        workshopUser.setName("Test Workshop User");
        workshopUser.setUsername("test_workshop_user_001");
        workshopUser.setEmail("test_workshop_user_001@example.com");
        workshopUser.setPassword("test@123");
        workshopUserRepository.save(workshopUser);


        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);
            workshopUserService.openWorkshop(workshopUser.getUsername());
            AddressRequest addressRequest = new AddressRequest();
            addressRequest.setLatitude(22.597693666787432);
            addressRequest.setLongitude(88.35945631449115);
            workshopUserService.addAddress(addressRequest);
            SecurityContextHolder.clearContext();
        }

    }

    @Order(1)
    @Test
    @DisplayName("AppUser Requesting a booking")
    void requestBookingTest(){
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AddVehicleResponse addVehicleResponse = vehicleService.addFourWheeler(new AddVehicleRequest("Honda", "Civic", 1700));
        vehicleId = addVehicleResponse.getVehicleId();

        bookingId = bookingService.requestBooking(workshopUser.getId(), WorkshopServiceType.TYRE_REPLACEMENT, vehicleId);
        assertNotNull(bookingId); // Check booking_id is created

        // Check booking exists in the repo
        assertTrue(bookingRepository.existsById(bookingId));

        SecurityContextHolder.clearContext();
    }

    @Order(2)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - STARTED")
    void toCheckStartedBookingStatus() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        BookingResponse b = bookingService.checkBookingStatus(bookingId);
        assertEquals(BookingStatus.STARTED, b.getCurrentStatus());
        assertEquals(400, b.getAmount());
        assertEquals("Civic", b.getVehicleDetails().getModel());
        assertNotNull(b.getBookingStartedTime());

        SecurityContextHolder.clearContext();
    }

    @Order(3)
    @Test
    @DisplayName("WorkshopUser Checking the booking queue")
    void workShopCheckBookingsQueue() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        List<BookingResponse> bookingResponseList = bookingService.getStartedBookings();
        assertEquals(1, bookingResponseList.size());
        assertEquals(bookingId, bookingResponseList.getFirst().getId());

        SecurityContextHolder.clearContext();
    }

    @Order(4)
    @Test
    @DisplayName("WorkshopUser Accept the Booking")
    void workShopAcceptBooking() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        BookingResponse bookingResponse = bookingService.acceptBooking(bookingId);
        assertEquals(bookingId, bookingResponse.getId());
        assertEquals(BookingStatus.BOOKED, bookingResponse.getCurrentStatus());

        SecurityContextHolder.clearContext();
    }

    @Order(5)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - BOOKED")
    void toCheckBookedBookingStatus() {
        defaultMethodToCheckBookingStatusForAppUser(BookingStatus.BOOKED);
    }

    @Order(6)
    @Test
    @DisplayName("AppUser Starts The Journey")
    void appUserStartJourney() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);
        BookingResponse bookingResponse = bookingService.startJourney(bookingId);
        assertEquals(bookingId, bookingResponse.getId());
        assertEquals(BookingStatus.ON_THE_WAY, bookingResponse.getCurrentStatus());

        SecurityContextHolder.clearContext();
    }

    @Order(7)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - ON_THE_WAY")
    void toCheckOnTheWayBookingStatus() {
        defaultMethodToCheckBookingStatusForAppUser(BookingStatus.ON_THE_WAY);
    }

    @Order(8)
    @Test
    @DisplayName("AppUser Requesting for OTP and WorkShopUser Updating Status to Waiting")
    void toStartWaiting() {
        BookingStatusResponse bookingStatusResponse = null;
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            bookingStatusResponse = bookingService.generateBookingOtp(bookingId);
            SecurityContextHolder.clearContext();
        }
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            initiatePaymentResponse = paymentService.initiatePayment(bookingId);
            Optional<Booking> tempBooking = bookingRepository.findById(bookingId);
            if(tempBooking.isPresent()) {
                tempBooking.get().setCurrentPaymentStatus(PaymentStatus.PAID);
                bookingRepository.save(tempBooking.get());
            }

            bookingService.verifyOtpAndSetStatus(
                    new BookingRequestOtp(bookingId, bookingStatusResponse.getOtp()), BookingStatus.WAITING);

            SecurityContextHolder.clearContext();
        }
    }

    @Order(9)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - WAITING")
    void toCheckWaitingBookingStatus() {
        defaultMethodToCheckBookingStatusForAppUser(BookingStatus.WAITING);
    }

    @Order(10)
    @Test
    @DisplayName("AppUser Requesting for OTP and WorkShopUser Updating Status to Repairing")
    void toStartRepairing() {
        BookingStatusResponse bookingStatusResponse = null;
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            bookingStatusResponse = bookingService.generateBookingOtp(bookingId);

            SecurityContextHolder.clearContext();
        }
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            bookingService.verifyOtpAndSetStatus(
                    new BookingRequestOtp(bookingId, bookingStatusResponse.getOtp()), BookingStatus.REPAIRING);

            SecurityContextHolder.clearContext();
        }
    }

    @Order(11)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - REPAIRING")
    void toCheckRepairingBookingStatus() {
        defaultMethodToCheckBookingStatusForAppUser(BookingStatus.REPAIRING);
    }

    @Order(12)
    @Test
    @DisplayName("AppUser Requesting for OTP and WorkShopUser Updating Status to Completed")
    void toComplete() {
        BookingStatusResponse bookingStatusResponse = null;
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            bookingStatusResponse = bookingService.generateBookingOtp(bookingId);

            SecurityContextHolder.clearContext();
        }
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            bookingService.verifyOtpAndSetStatus(
                    new BookingRequestOtp(bookingId, bookingStatusResponse.getOtp()), BookingStatus.COMPLETED);

            SecurityContextHolder.clearContext();
        }
    }

    @Order(13)
    @Test
    @DisplayName("AppUser Checking the bookingStatus - COMPLETED")
    void toCheckCompletedBookingStatus() {
        defaultMethodToCheckBookingStatusForAppUser(BookingStatus.COMPLETED);
    }

    void defaultMethodToCheckBookingStatusForAppUser(BookingStatus bookingStatus) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        BookingResponse b = bookingService.checkBookingStatus(bookingId);
        assertEquals(bookingStatus, b.getCurrentStatus());

        SecurityContextHolder.clearContext();
    }

    @Order(14)
    @Test
    @DisplayName("Checking Rating for AppUser")
    void checkRatingForAppUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        int ratingToAppUser = 4;

        bookingService.giveRatingToAppUser(new AppUserRatingRequest(bookingId, ratingToAppUser));

        assertThrows(RuntimeException.class, () -> {
            bookingService.giveRatingToAppUser(new AppUserRatingRequest(bookingId, ratingToAppUser));
        });

        Booking booking = bookingService.getBookingById(bookingId);
        assertEquals(ratingToAppUser, booking.getRatingWorkshopToAppUser());
        appUser = appUserService.getAppUserById(appUser.getId());
        assertEquals(1, appUser.getRatingsList().size());
        assertEquals(ratingToAppUser, appUser.getRatingsList().getFirst());

        SecurityContextHolder.clearContext();
    }

    @Order(15)
    @Test
    @DisplayName("Checking Rating for WorkShopUser")
    void checkRatingForWorkShopUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        int ratingToWorkShopUser = 4;

        bookingService.giveRatingToWorkShopUser(new WorkShopUserRatingRequest(bookingId, ratingToWorkShopUser));

        assertThrows(RuntimeException.class, () -> {
            bookingService.giveRatingToWorkShopUser(new WorkShopUserRatingRequest(bookingId, ratingToWorkShopUser));
        });

        Booking booking = bookingService.getBookingById(bookingId);
        assertEquals(ratingToWorkShopUser, booking.getRatingAppUserToWorkshop());
        workshopUser = workshopUserService.getWorkshopUserById(booking.getWorkshopUserId());
        assertEquals(1, workshopUser.getRatingsList().size());
        assertEquals(ratingToWorkShopUser, workshopUser.getRatingsList().getFirst());

        SecurityContextHolder.clearContext();
    }

    @AfterAll
    public void tearDownAll() {
        SecurityContextHolder.clearContext();
        workshopUserRepository.deleteById(workshopUser.getId());
        appUserRepository.deleteById(appUser.getId());
        bookingRepository.deleteById(bookingId);
        paymentRepository.deleteById(initiatePaymentResponse.getPaymentId());
    }
}