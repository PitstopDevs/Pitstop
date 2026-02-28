package com.pitstop.app.service;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.WorkshopUserFilterRequest;
import com.pitstop.app.dto.WorkshopUserFilterResponse;
import com.pitstop.app.model.WorkshopUser;

import java.util.List;

public interface WorkshopSearchService {
    List<WorkshopUserFilterResponse> filterWorkshopUsers(WorkshopUserFilterRequest workshopUserRequest);
    List<WorkshopServiceType> getAvailableServices(VehicleType vehicleType);
}
