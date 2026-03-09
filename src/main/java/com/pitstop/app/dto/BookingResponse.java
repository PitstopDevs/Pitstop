package com.pitstop.app.dto;

import com.pitstop.app.constants.BookingStatus;
import com.pitstop.app.constants.PaymentStatus;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.model.Address;
import com.pitstop.app.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BookingResponse {
    private String id;
    private double amount;
    private VehicleDetailsResponse vehicleDetails;
    private BookingStatus currentStatus;
    private LocalDateTime bookingStartedTime;
    private LocalDateTime bookingCompletedTime;
    private String workShopId;
    private String workShopName;
    private Address workShopAddress;
    private PaymentStatus paymentStatus;

    private String customerName;
    private WorkshopServiceType serviceType;
}
