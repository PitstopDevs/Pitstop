package com.pitstop.app.controller;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.WorkshopUserFilterRequest;
import com.pitstop.app.dto.WorkshopUserFilterResponse;
import com.pitstop.app.service.impl.WorkshopSearchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
public class WorkshopSearchController {
    private final WorkshopSearchServiceImpl workshopSearchService;

    @PostMapping("/filterWorkshops")
    public ResponseEntity<?> filterWorkshops(@RequestBody WorkshopUserFilterRequest workshopUserRequest){
        try{
            List<WorkshopUserFilterResponse> results = workshopSearchService.filterWorkshopUsers(workshopUserRequest);
            return ResponseEntity.ok().body(results);
        }
        catch(Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/available-services")
    public ResponseEntity<List<WorkshopServiceType>> getAvailableServices(@RequestParam VehicleType vehicleType) {
        return ResponseEntity.ok(workshopSearchService.getAvailableServices(vehicleType));
    }
}
