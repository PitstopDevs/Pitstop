package com.pitstop.app.service.impl;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.*;
import com.pitstop.app.model.*;
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
public class BookingServiceCancellationScenariosForAppUserTest {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private AppUserServiceImpl appUserService;

    @Autowired
    private WorkshopUserServiceImpl workshopUserService;

    @Autowired
    private BookingHistoryServiceImpl bookingHistoryService;

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

    // Booking id to check cancellation By AppUser (STARTED -> CANCELLED_BY_APPUSER)
    private String startedBookingId;

    // Booking id to check cancellation By WorkShopUser (BOOKED -> CANCELLED_BY_APPUSER)
    private String bookedBookingId;

    // Booking id to check cancellation By AppUser (ON_THE_WAY -> CANCELLED_BY_APPUSER)
    private String onTheWayBookingId;

    // Booking id to check cancellation By WorkShopUser (WAITING -> CANCELLED_BY_APPUSER)
    private String waitingBookingId;

    private String vehicleId;

    private InitiatePaymentResponse initiatePaymentResponse = null;


    @BeforeAll
    public void setUpOnce() {
        workshopUserRepository.deleteByUsername("test_workshop_user_002");
        appUserRepository.deleteByUsername("test_app_user_002");
        // AppUser Set-Up
        appUser = new AppUser();
        appUser.setName("Test App User");
        appUser.setUsername("test_app_user_002");
        appUser.setEmail("test_app_user_002@example.com");
        appUser.setPassword("test@123");
        appUserRepository.save(appUser);

        // Adding Address and Vehicle
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);
            AddressRequest addressRequest = new AddressRequest();
            addressRequest.setLatitude(22.597693666787432);
            addressRequest.setLongitude(88.35945631449115);
            appUserService.addAddress(addressRequest);

            AddVehicleResponse addVehicleResponse = vehicleService.addFourWheeler(new AddVehicleRequest("Honda", "Civic", 1700));
            vehicleId = addVehicleResponse.getVehicleId();

            SecurityContextHolder.clearContext();
        }

        // WorkshopUser Set-Up
        workshopUser = new WorkshopUser();
        workshopUser.setName("Test Workshop User");
        workshopUser.setUsername("test_workshop_user_002");
        workshopUser.setEmail("test_workshop_user_002@example.com");
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

        // Setting up the bookings


        appUser = appUserService.getAppUserByUsername("test_app_user_002");
        workshopUser = workshopUserService.getWorkshopUserByUsername("test_workshop_user_002");

        // BookingStatus = STARTED
        {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
            SecurityContextHolder.getContext().setAuthentication(auth);

            startedBookingId = bookingService.requestBooking(workshopUser.getId(), WorkshopServiceType.TYRE_REPLACEMENT, vehicleId);
            assertNotNull(startedBookingId); // Check startedBookingId is created
            SecurityContextHolder.clearContext();
        }

        // BookingStatus = BOOKED
        {
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);

                bookedBookingId = bookingService.requestBooking(workshopUser.getId(), WorkshopServiceType.TYRE_REPLACEMENT, vehicleId);
                assertNotNull(bookedBookingId); // Check bookedBookingId is created
                SecurityContextHolder.clearContext();
            }
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);
                BookingResponse bookingResponse = bookingService.acceptBooking(bookedBookingId);
                assertEquals(bookedBookingId, bookingResponse.getId());
                assertEquals(BookingStatus.BOOKED, bookingResponse.getCurrentStatus());
                SecurityContextHolder.clearContext();
            }
        }

        // BookingStatus = ON_THE_WAY
        {
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);

                onTheWayBookingId = bookingService.requestBooking(workshopUser.getId(), WorkshopServiceType.TYRE_REPLACEMENT, vehicleId);
                assertNotNull(onTheWayBookingId); // Check onTheWayBookingId is created
                SecurityContextHolder.clearContext();
            }
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);
                BookingResponse bookingResponse = bookingService.acceptBooking(onTheWayBookingId);
                assertEquals(onTheWayBookingId, bookingResponse.getId());
                assertEquals(BookingStatus.BOOKED, bookingResponse.getCurrentStatus());
                SecurityContextHolder.clearContext();
            }
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);
                BookingResponse bookingResponse = bookingService.startJourney(onTheWayBookingId);
                assertEquals(onTheWayBookingId, bookingResponse.getId());
                assertEquals(BookingStatus.ON_THE_WAY, bookingResponse.getCurrentStatus());
                SecurityContextHolder.clearContext();
            }
        }

        // BookingStatus = WAITING
        {
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);

                waitingBookingId = bookingService.requestBooking(workshopUser.getId(), WorkshopServiceType.TYRE_REPLACEMENT, vehicleId);
                assertNotNull(waitingBookingId); // Check waitingBookingId is created
                SecurityContextHolder.clearContext();
            }
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);
                BookingResponse bookingResponse = bookingService.acceptBooking(waitingBookingId);
                assertEquals(waitingBookingId, bookingResponse.getId());
                assertEquals(BookingStatus.BOOKED, bookingResponse.getCurrentStatus());
                SecurityContextHolder.clearContext();
            }
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                SecurityContextHolder.getContext().setAuthentication(auth);
                BookingResponse bookingResponse = bookingService.startJourney(waitingBookingId);
                assertEquals(waitingBookingId, bookingResponse.getId());
                assertEquals(BookingStatus.ON_THE_WAY, bookingResponse.getCurrentStatus());
                SecurityContextHolder.clearContext();
            }
            {
                BookingStatusResponse bookingStatusResponse = null;
                {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    bookingStatusResponse = bookingService.generateBookingOtp(waitingBookingId);
                    SecurityContextHolder.clearContext();
                }
                {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(workshopUser.getUsername(), workshopUser.getPassword());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    initiatePaymentResponse = paymentService.initiatePayment(waitingBookingId);
                    Optional<Booking> tempBooking = bookingRepository.findById(waitingBookingId);
                    if(tempBooking.isPresent()) {
                        tempBooking.get().setCurrentPaymentStatus(PaymentStatus.PAID);
                        bookingRepository.save(tempBooking.get());
                    }

                    bookingService.verifyOtpAndSetStatus(
                            new BookingRequestOtp(waitingBookingId, bookingStatusResponse.getOtp()), BookingStatus.WAITING);

                    SecurityContextHolder.clearContext();
                }
            }
        }
    }

    @Order(1)
    @Test
    @DisplayName("Check the bookingHistoryImpl for AppUser")
    void bookingHistoryImplCheckAppUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        List<AppUserBookingHistoryResponse> bookingHistoryForAppUser = bookingHistoryService.getBookingHistoryForAppUser();
        assertEquals(4, bookingHistoryForAppUser.size());

        SecurityContextHolder.clearContext();
    }

    @Order(2)
    @Test
    @DisplayName("Negative Testing Scenario 1: STARTED -> ON_THE_WAY")
    void negativeTestingStartedToOnTheWay() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.startJourney(startedBookingId);
        });

        SecurityContextHolder.clearContext();
    }

    @Order(3)
    @Test
    @DisplayName("Cancel STARTED BOOKING and Check")
    void toCancelStartedBooking() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        bookingService.cancelBookingByAppUser(startedBookingId);
        Booking currentBooking = bookingService.getBookingById(startedBookingId);
        assertEquals(BookingStatus.CANCELLED_BY_APPUSER, currentBooking.getCurrentStatus());
        assertEquals(true, currentBooking.getAppUserEligibleForRefund());

        SecurityContextHolder.clearContext();
    }
    @Order(4)
    @Test
    @DisplayName("Cancel BOOKED BOOKING and Check")
    void toCancelBookedBooking() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        bookingService.cancelBookingByAppUser(bookedBookingId);
        Booking currentBooking = bookingService.getBookingById(bookedBookingId);
        assertEquals(BookingStatus.CANCELLED_BY_APPUSER, currentBooking.getCurrentStatus());
        assertEquals(false, currentBooking.getAppUserEligibleForRefund());

        SecurityContextHolder.clearContext();
    }

    @Order(5)
    @Test
    @DisplayName("Cancel ON_THE_WAY BOOKING and Check")
    void toCancelOnTheWayBooking() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        bookingService.cancelBookingByAppUser(onTheWayBookingId);
        Booking currentBooking = bookingService.getBookingById(onTheWayBookingId);
        assertEquals(BookingStatus.CANCELLED_BY_APPUSER, currentBooking.getCurrentStatus());
        assertEquals(false, currentBooking.getAppUserEligibleForRefund());

        SecurityContextHolder.clearContext();
    }

    @Order(6)
    @Test
    @DisplayName("Negative Testing Scenario 2: WAITING -> ON_THE_WAY")
    void negativeTestingWaitingToBooked() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.startJourney(waitingBookingId);
        });

        SecurityContextHolder.clearContext();
    }

    @Order(7)
    @Test
    @DisplayName("Cancel WAITING BOOKING and Check")
    void toCancelWaitingBooking() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        bookingService.cancelBookingByAppUser(waitingBookingId);
        Booking currentBooking = bookingService.getBookingById(waitingBookingId);
        assertEquals(BookingStatus.CANCELLED_BY_APPUSER, currentBooking.getCurrentStatus());
        assertEquals(false, currentBooking.getAppUserEligibleForRefund());

        SecurityContextHolder.clearContext();
    }

    @Order(8)
    @Test
    @DisplayName("Negative Testing Scenario 3: CANCELLED_BY_APPUSER -> ON_THE_WAY")
    void negativeTestingCancelToOnTheWay() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.startJourney(waitingBookingId);
        });

        SecurityContextHolder.clearContext();
    }

    @AfterAll
    public void tearDownAll() {
        SecurityContextHolder.clearContext();
        workshopUserRepository.deleteById(workshopUser.getId());
        appUserRepository.deleteById(appUser.getId());
        bookingRepository.deleteById(startedBookingId);
        bookingRepository.deleteById(bookedBookingId);
        bookingRepository.deleteById(onTheWayBookingId);
        bookingRepository.deleteById(waitingBookingId);
        paymentRepository.deleteById(initiatePaymentResponse.getPaymentId());
    }
}
