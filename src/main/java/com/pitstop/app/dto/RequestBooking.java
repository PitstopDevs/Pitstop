package com.pitstop.app.dto;

import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RequestBooking {
    private String appUserId;
    private String workShopUserId;
    private WorkshopServiceType serviceType;
    private String vehicleId;
}
