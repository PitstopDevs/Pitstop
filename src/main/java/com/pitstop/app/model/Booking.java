package com.pitstop.app.model;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.WorkshopServiceType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "bookings")
@NoArgsConstructor
public class Booking {

    @Id
    private String id;
    private double amount;
    private Vehicle vehicle;

    private String workShopName;
    private Address workShopAddress;
    private String otp;
    private LocalDateTime otpExpiry;

    private BookingStatus currentStatus = BookingStatus.STARTED;
    private PaymentStatus currentPaymentStatus = PaymentStatus.NOT_PAID;
    private LocalDateTime bookingStartedTime = LocalDateTime.now();
    private LocalDateTime bookingCompletedTime;

    private int ratingAppUserToWorkshop = 0;
    private int ratingWorkshopToAppUser = 0;

    //Lets you easily link Booking to an AppUser and WorkshopUser if you plan to display booking history for each.
//    @DBRef
//    private AppUser appUser;
//
//    @DBRef
//    private WorkshopUser workshopUser;

    @Indexed
    private String appUserId;

    @Indexed
    private String workshopUserId;

    List<BookingStatusWithTimeStamp> bookingStatusHistory;
    Boolean appUserEligibleForRefund = null;

    private WorkshopServiceType serviceType;

    public Booking(double amount, Vehicle vehicle, String appUserId,String workshopUserId,WorkshopServiceType serviceType) {
        this.amount = amount;
        this.vehicle = vehicle;
        this.appUserId = appUserId;
        this.bookingStatusHistory = new ArrayList<>();
        bookingStatusHistory.add(new BookingStatusWithTimeStamp(currentStatus, LocalDateTime.now()));
        this.workshopUserId = workshopUserId;
        this.serviceType = serviceType;
    }
}
