package com.pitstop.app.service.impl;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.constants.WorkshopStatus;
import com.pitstop.app.dto.*;
import com.pitstop.app.model.*;
import com.pitstop.app.repository.BookingRepository;
import com.pitstop.app.repository.VehicleRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import com.pitstop.app.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AppUserServiceImpl appUserService;
    private final WorkshopUserServiceImpl workshopUserService;
    private final WorkshopUserRepository workshopUserRepository;
    private final OTPService otpService;
    private final VehicleRepository vehicleRepository;
    private final AdminPricingServiceImpl adminPricingService;

    @Override
    public Booking saveBookingDetails(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingById(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Booking not found with ID :"+id));
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public void deleteBooking(String id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    public List<WorkshopStatusResponse> getAllOpenWorkshops() {
        List<WorkshopUser> allWorkShops = workshopUserService.getAllWorkshopUser();
        List<WorkshopStatusResponse> openWorkShops = new ArrayList<>();
        for(WorkshopUser workshopUser : allWorkShops) {
            if(workshopUser.getCurrentWorkshopStatus() == WorkshopStatus.OPEN) {
                openWorkShops.add(new WorkshopStatusResponse(workshopUser.getId(), workshopUser.getName(),
                        workshopUser.getUsername(),workshopUser.getCurrentWorkshopStatus(), workshopUser.getWorkshopAddress()));
            }
        }
        return openWorkShops;
    }

    public String requestBooking(String workShopUserId,
                                 WorkshopServiceType serviceType, String vehicleId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        // 2. Throw exception if workshopUser doesn't exist and catch it in the controller layer
        // and return relevant response.
        WorkshopUser workshopUser = workshopUserService.getWorkshopUserById(workShopUserId);
        if(workshopUser == null) {
            throw new RuntimeException("The requested workShop was not found! Id = "+workShopUserId);
        } else if(workshopUser.getCurrentWorkshopStatus() != WorkshopStatus.OPEN) {
            throw new RuntimeException("The requested workShop is closed now! Id = "+workShopUserId);
        }

        boolean exists = currentAppUser.getVehicleList().stream()
                .anyMatch(v -> v.getId().equals(vehicleId));

        if(!exists) {
            log.error("Requested Vehicle ID for deletion does not belongs to logged in user. Vehicle id :: {}", vehicleId);
            throw new RuntimeException("Requested Vehicle ID for deletion does not belongs to logged in user.");
        }

        Optional<Vehicle> v = vehicleRepository.findById(vehicleId);
        if(v.isEmpty()) {
            log.error("Requested Vehicle ID not found. Vehicle id :: {}", vehicleId);
            throw new RuntimeException("Requested Vehicle ID not found.");
        }

        PricingRuleResponse priceRule = adminPricingService.getPricingRuleByVehicleTypeAndServiceType(v.get().getVehicleType(), serviceType);

        double amount = priceRule.getAmount();
        if(workshopUser.isPremiumWorkshop())
            amount += priceRule.getPremiumAmount();

        Booking booking = bookingRepository.save(new Booking(amount, v.get(), currentAppUser.getId()));

        currentAppUser.getBookingHistory().add(booking);
        workshopUser.getBookingHistory().add(booking);

        appUserService.updateAppUserDetails(currentAppUser);
        workshopUserService.updateWorkshopUserDetails(workshopUser);

        return booking.getId();
    }

    public BookingResponse checkBookingStatus(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        if(!currentAppUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(id))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+id);
        }

        Booking currentBooking = getBookingById(id);
        if(currentBooking == null) {
            throw new RuntimeException("Booking id not found, id = "+id);
        }

        if(currentBooking.getCurrentStatus() == BookingStatus.STARTED)
            return new BookingResponse(currentBooking.getId(), currentBooking.getAmount(),
                    new VehicleDetailsResponse(currentBooking.getVehicle().getId(), currentBooking.getVehicle().getVehicleType(),
                            currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                null, null, null,currentBooking.getCurrentPaymentStatus());

        else
            return new BookingResponse(currentBooking.getId(), currentBooking.getAmount(), new VehicleDetailsResponse(currentBooking.getVehicle().getId(), currentBooking.getVehicle().getVehicleType(),
                    currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                    currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                    currentBooking.getWorkshopUserId(), currentBooking.getWorkShopName(), currentBooking.getWorkShopAddress(),currentBooking.getCurrentPaymentStatus());
    }

    public List<BookingResponse> getStartedBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found"));

        List<Booking> allBookings = currentWorkShopUser.getBookingHistory();
        List<BookingResponse> startedBookings = new ArrayList<>();
        for(Booking currentBooking : allBookings) {
            if(currentBooking.getCurrentStatus() == BookingStatus.STARTED) {
                startedBookings.add(new BookingResponse(currentBooking.getId(), currentBooking.getAmount(), new VehicleDetailsResponse(currentBooking.getVehicle().getId(),
                        currentBooking.getVehicle().getVehicleType(),
                        currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                        currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                        null, null, null,currentBooking.getCurrentPaymentStatus()));
            }
        }
        return startedBookings;
    }

    public BookingResponse acceptBooking(String bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found"));

        if(!currentWorkShopUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingId))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingId);
        }

        Booking currentBooking = getBookingById(bookingId);
        if (!currentBooking.getCurrentStatus().canTransitionTo(BookingStatus.BOOKED)) {
            throw new IllegalArgumentException(
                    String.format("Invalid booking status transition: %s -> %s", currentBooking.getCurrentStatus(), BookingStatus.BOOKED)
            );
        }
        currentBooking.setCurrentStatus(BookingStatus.BOOKED);
        currentBooking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.BOOKED, LocalDateTime.now()));
        currentBooking.setWorkshopUserId(currentWorkShopUser.getId());
        currentBooking.setWorkShopName(currentWorkShopUser.getName());
        currentBooking.setWorkShopAddress(currentWorkShopUser.getWorkshopAddress());

        saveBookingDetails(currentBooking);

        return new BookingResponse(currentBooking.getId(), currentBooking.getAmount(), new VehicleDetailsResponse(currentBooking.getVehicle().getId(),
                currentBooking.getVehicle().getVehicleType(),
                currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                currentBooking.getWorkshopUserId(), currentWorkShopUser.getName(), currentWorkShopUser.getWorkshopAddress(),currentBooking.getCurrentPaymentStatus());
    }

    public BookingResponse rejectBooking(String bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found"));

        if(!currentWorkShopUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingId))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingId);
        }

        Booking currentBooking = getBookingById(bookingId);
        // Temporary Logic implemented for now
        // In reality after booking is rejected, user should get prompt to request booking in another workshop
        if(currentBooking.getCurrentStatus() != BookingStatus.BOOKED)
            throw new RuntimeException("Booking id provided cannot be set REJECTED, id = "+bookingId);

        currentBooking.setCurrentStatus(BookingStatus.REJECTED);
        currentBooking.setWorkshopUserId(currentWorkShopUser.getId());

        saveBookingDetails(currentBooking);

        return new BookingResponse(currentBooking.getId(), currentBooking.getAmount(), new VehicleDetailsResponse(currentBooking.getVehicle().getId(),
                currentBooking.getVehicle().getVehicleType(),
                currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                currentBooking.getWorkshopUserId(), currentWorkShopUser.getName(), currentWorkShopUser.getWorkshopAddress(),currentBooking.getCurrentPaymentStatus());
    }

    public BookingResponse startJourney(String bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        if(!currentAppUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingId))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingId);
        }

        Booking currentBooking = getBookingById(bookingId);

        if (!currentBooking.getCurrentStatus().canTransitionTo(BookingStatus.ON_THE_WAY)) {
            throw new IllegalArgumentException(
                    String.format("Invalid booking status transition: %s -> %s", currentBooking.getCurrentStatus(), BookingStatus.ON_THE_WAY)
            );
        }
        currentBooking.setCurrentStatus(BookingStatus.ON_THE_WAY);
        currentBooking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.ON_THE_WAY, LocalDateTime.now()));

        saveBookingDetails(currentBooking);

        return new BookingResponse(currentBooking.getId(), currentBooking.getAmount(), new VehicleDetailsResponse(currentBooking.getVehicle().getId(),
                currentBooking.getVehicle().getVehicleType(),
                currentBooking.getVehicle().getBrand(), currentBooking.getVehicle().getModel(), currentBooking.getVehicle().getEngineCapacity()),
                currentBooking.getCurrentStatus(), currentBooking.getBookingStartedTime(), currentBooking.getBookingCompletedTime(),
                currentBooking.getWorkshopUserId(), currentBooking.getWorkShopName(), currentBooking.getWorkShopAddress(),currentBooking.getCurrentPaymentStatus());
    }

    public BookingStatusResponse generateBookingOtp(String bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        if(!currentAppUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingId))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));


        String otp = otpService.generateOtp();
        LocalDateTime expiry = otpService.getExpiryTime();

        booking.setOtp(otp);
        booking.setOtpExpiry(expiry);

        bookingRepository.save(booking);
        return new BookingStatusResponse(bookingId, booking.getCurrentStatus(), otp);
    }

    public void verifyOtpAndSetStatus(BookingRequestOtp bookingRequestOtp, BookingStatus bookingStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("User not found"));

        if(!currentWorkShopUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingRequestOtp.getId()))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingRequestOtp.getId());
        }

        Booking booking = bookingRepository.findById(bookingRequestOtp.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if(bookingStatus == BookingStatus.WAITING ||  bookingStatus == BookingStatus.REPAIRING || bookingStatus == BookingStatus.COMPLETED) {
            if(booking.getCurrentPaymentStatus() != PaymentStatus.PAID) {
                throw new RuntimeException("Booking status "+ bookingStatus +" cannot be set as payment status is " + booking.getCurrentPaymentStatus());
            }
        }

        if (!booking.getCurrentStatus().canTransitionTo(bookingStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid booking status transition: %s -> %s", booking.getCurrentStatus(), bookingStatus)
            );
        }

        if (otpService.isOtpExpired(booking.getOtpExpiry())) {
            throw new RuntimeException("OTP expired. Please regenerate.");
        }

        if (!booking.getOtp().equals(bookingRequestOtp.getOtp())) {
            throw new RuntimeException("Invalid OTP.");
        }

        booking.setCurrentStatus(bookingStatus);
        booking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(bookingStatus, LocalDateTime.now()));
        booking.setOtp(null);

        if(bookingStatus == BookingStatus.COMPLETED)
            booking.setBookingCompletedTime(LocalDateTime.now());

        bookingRepository.save(booking);
    }

    public void cancelBookingByAppUser(String bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        if(!currentAppUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingId))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));


        if (!booking.getCurrentStatus().canTransitionTo(BookingStatus.CANCELLED_BY_APPUSER)) {
            throw new IllegalArgumentException(
                    String.format("Invalid booking status transition: %s -> %s", booking.getCurrentStatus(), BookingStatus.CANCELLED_BY_APPUSER)
            );
        }

        booking.setAppUserEligibleForRefund(booking.getCurrentStatus() == BookingStatus.STARTED);
        booking.setCurrentStatus(BookingStatus.CANCELLED_BY_APPUSER);
        booking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.CANCELLED_BY_APPUSER, LocalDateTime.now()));

        bookingRepository.save(booking);
    }

    public void cancelBookingByWorkshopUser(BookingRequestOtp bookingRequestOtp) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!currentWorkShopUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(bookingRequestOtp.getId()))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = " + bookingRequestOtp.getId());
        }

        Booking booking = bookingRepository.findById(bookingRequestOtp.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (otpService.isOtpExpired(booking.getOtpExpiry())) {
            throw new RuntimeException("OTP expired. Please regenerate.");
        }

        if (!booking.getOtp().equals(bookingRequestOtp.getOtp())) {
            throw new RuntimeException("Invalid OTP.");
        }

        if (!booking.getCurrentStatus().canTransitionTo(BookingStatus.CANCELLED_BY_WORKSHOPUSER)) {
            throw new IllegalArgumentException(
                    String.format("Invalid booking status transition: %s -> %s", booking.getCurrentStatus(), BookingStatus.CANCELLED_BY_WORKSHOPUSER)
            );
        }

        if(booking.getCurrentStatus() == BookingStatus.STARTED) {
            booking.setCurrentStatus(BookingStatus.REJECTED);
            booking.setWorkshopUserId(currentWorkShopUser.getId());
            booking.setWorkShopName(currentWorkShopUser.getName());
            booking.setWorkShopAddress(currentWorkShopUser.getWorkshopAddress());
            booking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.REJECTED, LocalDateTime.now()));
        } else if(booking.getCurrentStatus() == BookingStatus.REPAIRING) {
            booking.setCurrentStatus(BookingStatus.INCOMPLETE);
            booking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.INCOMPLETE, LocalDateTime.now()));
        } else {
            booking.setCurrentStatus(BookingStatus.CANCELLED_BY_WORKSHOPUSER);
            booking.getBookingStatusHistory().add(new BookingStatusWithTimeStamp(BookingStatus.CANCELLED_BY_WORKSHOPUSER, LocalDateTime.now()));
        }
        booking.setAppUserEligibleForRefund(true);
        booking.setOtp(null);
        bookingRepository.save(booking);
    }

    public void giveRatingToAppUser(AppUserRatingRequest appUserRatingRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!currentWorkShopUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(appUserRatingRequest.getBookingId()))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = " + appUserRatingRequest.getBookingId());
        }

        Booking booking = bookingRepository.findById(appUserRatingRequest.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if(booking.getCurrentStatus() != BookingStatus.COMPLETED)
            throw new RuntimeException("Ratings cannot be set unless the booking is in COMPLETED state.");

        if(booking.getRatingWorkshopToAppUser() > 0) {
            throw new RuntimeException("Rating already added to AppUser for bookingId = "+booking.getId());
        }

        booking.setRatingWorkshopToAppUser(appUserRatingRequest.getRating());
        AppUser appUser = appUserService.getAppUserById(booking.getAppUserId());

        if(appUser.getRatingsList().size() < 5) {
            appUser.getRatingsList().add(appUserRatingRequest.getRating());
        } else if(appUser.getRatingsList().size() == 5){
            double total  = appUser.getRating();
            appUser.getRatingsList().add(appUserRatingRequest.getRating());
            for(int n : appUser.getRatingsList()) {
                total += n;
            }
            appUser.setRating((double) total / 7);
        } else  {
            double total = appUser.getRating() * appUser.getRatingsList().size();
            total += appUserRatingRequest.getRating();
            appUser.getRatingsList().add(appUserRatingRequest.getRating());
            double newRating = (double) total /  appUser.getRatingsList().size();
            appUser.setRating(newRating);
        }
        bookingRepository.save(booking);
        appUserService.updateAppUserDetails(appUser);
    }

    public void giveRatingToWorkShopUser(WorkShopUserRatingRequest workShopUserRatingRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser currentAppUser = appUserService.getAppUserByUsername(username);

        if(!currentAppUser.getBookingHistory()
                .stream()
                .anyMatch(b -> b.getId().equals(workShopUserRatingRequest.getBookingId()))) {
            throw new RuntimeException("Booking id provided is not current user's booking, id = "+workShopUserRatingRequest.getBookingId());
        }

        Booking booking = bookingRepository.findById(workShopUserRatingRequest.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if(booking.getCurrentStatus() != BookingStatus.COMPLETED)
            throw new RuntimeException("Ratings cannot be set unless the booking is in COMPLETED state.");

        if(booking.getRatingAppUserToWorkshop() > 0) {
            throw new RuntimeException("Rating already added to WorkShopUser for bookingId = "+booking.getId());
        }

        booking.setRatingAppUserToWorkshop(workShopUserRatingRequest.getRating());
        WorkshopUser workShopUser = workshopUserService.getWorkshopUserById(booking.getWorkshopUserId());

        if(workShopUser.getRatingsList().size() < 5) {
            workShopUser.getRatingsList().add(workShopUserRatingRequest.getRating());
        } else if(workShopUser.getRatingsList().size() == 5){
            double total  = workShopUser.getRating();
            workShopUser.getRatingsList().add(workShopUserRatingRequest.getRating());
            for(int n : workShopUser.getRatingsList()) {
                total += n;
            }
            workShopUser.setRating((double) total / 7);
        } else  {
            double total = workShopUser.getRating() * workShopUser.getRatingsList().size();
            total += workShopUserRatingRequest.getRating();
            workShopUser.getRatingsList().add(workShopUserRatingRequest.getRating());
            double newRating = (double) total /  workShopUser.getRatingsList().size();
            workShopUser.setRating(newRating);
        }
        bookingRepository.save(booking);
        workshopUserService.updateWorkshopUserDetails(workShopUser);
    }
}
