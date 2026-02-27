package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.constants.WorkshopStatus;
import com.pitstop.app.dto.*;
import com.pitstop.app.exception.BusinessException;
import com.pitstop.app.exception.UserAlreadyExistException;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.model.*;
import com.pitstop.app.repository.WorkshopUserRepository;
import com.pitstop.app.service.WorkshopService;
import com.pitstop.app.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkshopUserServiceImpl implements WorkshopService {
    private final WorkshopUserRepository workshopUserRepository;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthenticationManager manager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    @Value("${trueway.api.url}")
    private String truewayApiUrl;

    @Value("${trueway.api.key}")
    private String truewayApiKey;

    @Value("${trueway.api.host}")
    private String truewayApiHost;

    @Value("${nominatim.api.url}")
    private String nominatimApiUrl;

    @Value("${nominatim.user.agent}")
    private String nominatimUserAgent;

    @Override
    public WorkshopUserRegisterResponse saveWorkshopUserDetails(WorkshopUserRegisterRequest workshopUserRequest) {
        boolean existsByUsername = workshopUserRepository.findByUsername(workshopUserRequest.getUsername()).isPresent();
        boolean existsByEmail = workshopUserRepository.findByEmail(workshopUserRequest.getEmail()).isPresent();

        if(existsByEmail || existsByUsername){
            throw new UserAlreadyExistException("WorkshopUser already exists");
        }
        WorkshopUser workshop = new WorkshopUser();
        workshop.setName(workshopUserRequest.getName());
        workshop.setEmail(workshopUserRequest.getEmail());
        workshop.setUsername(workshopUserRequest.getUsername());
        workshop.setPassword(passwordEncoder.encode(workshopUserRequest.getPassword()));
        workshop.setAccountCreationDateTime(LocalDateTime.now());
        workshopUserRepository.save(workshop);
        log.info("WorkshopUser {} successfully saved : ", workshopUserRequest.getUsername());

        WorkshopUserRegisterResponse response = new WorkshopUserRegisterResponse();
        response.setId(workshop.getId());
        response.setName(workshopUserRequest.getName());
        response.setUsername(workshopUserRequest.getUsername());
        response.setEmail(workshopUserRequest.getEmail());
        response.setMessage("Workshop user created successfully");
        return response;
    }
    public void updateWorkshopUserDetails(WorkshopUser workshopUser){
        if(workshopUser.getId() != null && workshopUserRepository.existsById(workshopUser.getId())){
            workshopUserRepository.save(workshopUser);
        }
        else {
            workshopUser.setPassword(passwordEncoder.encode(workshopUser.getPassword()));
            workshopUserRepository.save(workshopUser);
        }
    }

    @Override
    public WorkshopUser getWorkshopUserById(String id) {
        return workshopUserRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("WorkshopUser not found with ID :"+id));
    }

    @Override
    public List<WorkshopUser> getAllWorkshopUser() {
        return workshopUserRepository.findAll();
    }

    @Override
    public AddressResponse addAddress(AddressRequest request) {
        String username = SecurityContextHolder
                .getContext().getAuthentication().getName();

        WorkshopUser user = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address finalAddress;

        if (request.getLatitude() != null && request.getLongitude() != null) {

            AddressResponse geo = findAddressFromCoordinates(
                    request.getLatitude(), request.getLongitude());

            finalAddress = Address.builder()
                    .id(UUID.randomUUID().toString())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .formattedAddress(geo.getFormattedAddress())
                    .build();

        } else if (request.getFormattedAddress() != null) {

            AddressResponse geo = findCoordinatesFromAddress(
                    request.getFormattedAddress());

            finalAddress = Address.builder()
                    .id(UUID.randomUUID().toString())
                    .latitude(geo.getLatitude())
                    .longitude(geo.getLongitude())
                    .formattedAddress(geo.getFormattedAddress())
                    .build();

        } else {
            throw new BusinessException("Invalid address data");
        }

        user.setWorkshopAddress(finalAddress);
        user.setAccountLastModifiedDateTime(LocalDateTime.now());

        workshopUserRepository.save(user);

        return new AddressResponse(
                finalAddress.getId(),
                finalAddress.getLatitude(),
                finalAddress.getLongitude(),
                finalAddress.getFormattedAddress(),
                true // always default
        );
    }

    public WorkshopUser getWorkshopUserByUsername(String username) {
        return workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("WorkshopUser not found with username :"+username));
    }

    public WorkshopStatusResponse openWorkshop(String username) {
        WorkshopUser workshopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("Workshop not found with username : "+username));

        log.info("Attempting to open workshop : {}",username);
        workshopUser.setCurrentWorkshopStatus(WorkshopStatus.OPEN);
        updateWorkshopUserDetails(workshopUser);

        log.info("Workshop : {} , opened successfully",username);
        return new WorkshopStatusResponse(workshopUser.getId(), workshopUser.getName(),
                workshopUser.getUsername(),workshopUser.getCurrentWorkshopStatus(), workshopUser.getWorkshopAddress());
    }
    public WorkshopLoginResponse loginWorkshopUser(WorkshopLoginRequest req){
        log.info("Login attempt for WorkshopUser: {}", req.getUsername());

        try {
            manager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Invalid WorkshopUser credentials for {}", req.getUsername());
            throw new RuntimeException("Incorrect username or password");
        }

        CustomUserDetails user =
                (CustomUserDetails) userDetailsService.loadUserByUsername(req.getUsername());

        if (user.getUserType() != UserType.WORKSHOP_USER) {
            log.warn("User {} attempted Workshop login but is {}",
                    req.getUsername(), user.getUserType());
            throw new RuntimeException("Only Workshop Users can login here");
        }

        String token = jwtUtil.generateToken(user);

        log.info("WorkshopUser {} logged in successfully", req.getUsername());

        return new WorkshopLoginResponse(
                user.getUsername(),
                token,
                "Login successful"
        );
    }
    private AddressResponse findAddressFromCoordinates(double latitude, double longitude) {
        try {
            String url = String.format(
                    "%s?lat=%f&lon=%f&format=json&addressdetails=1",
                    nominatimApiUrl, latitude, longitude
            );

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", nominatimUserAgent);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());
            JSONObject address = json.optJSONObject("address");
            String formattedAddress = json.optString("display_name", "Address not found");

            if (json.has("display_name") && address != null) {
                return AddressResponse.builder()
                        .formattedAddress(formattedAddress)
                        .build();
            } else {
                return AddressResponse.builder()
                        .formattedAddress("Address not found")
                        .build();
            }
        } catch (Exception e) {
            log.error("Invalid coordinates {}", e.getMessage());
            return AddressResponse.builder()
                    .formattedAddress("Error fetching address: " + e.getMessage())
                    .build();
        }
    }
    public AddressResponse findCoordinatesFromAddress(String addressPlainText) {
        try {
            String encodedAddress = URLEncoder.encode(addressPlainText, StandardCharsets.UTF_8);
            String url = truewayApiUrl + "?address=" + encodedAddress;

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-rapidapi-key", truewayApiKey)
                    .header("x-rapidapi-host", truewayApiHost)
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray results = json.optJSONArray("results");
                if (results == null || results.isEmpty()) {
                    throw new RuntimeException("No results found for: " + addressPlainText);
                }

                JSONObject first = results.getJSONObject(0);
                JSONObject location = first.getJSONObject("location");

                return AddressResponse.builder()
                        .latitude(location.getDouble("lat"))
                        .longitude(location.getDouble("lng"))
                        .formattedAddress(first.optString("address", addressPlainText))
                        .build();
            } else {
                throw new RuntimeException("API Error: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error fetching coordinates for: " + addressPlainText + " | " + e.getMessage(), e);
        }
    }
    @Override
    public WorkshopUserResponse getWorkshopUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkshopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("Workshop not found"));

        WorkshopUserResponse workshopUserResponse = new WorkshopUserResponse();
        workshopUserResponse.setUsername(currentWorkshopUser.getUsername());
        workshopUserResponse.setEmail(currentWorkshopUser.getEmail());
        workshopUserResponse.setAddress(currentWorkshopUser.getWorkshopAddress());

        return workshopUserResponse;
    }

    @Override
    public String updateWorkshopUser(WorkshopUserRequest workshopUserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkshopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("Workshop not found"));

        if(workshopUserRequest.getName() != null) {
            currentWorkshopUser.setName(workshopUserRequest.getName());
        }
        if(workshopUserRequest.getEmail() != null) {
            currentWorkshopUser.setEmail(workshopUserRequest.getEmail());
        }
        if(workshopUserRequest.getUsername() != null) {
            currentWorkshopUser.setUsername(workshopUserRequest.getUsername());
        }

        updateWorkshopUserDetails(currentWorkshopUser);

        return "Workshop Details updated successfully";
    }

    @Override
    public void changePassword(ChangePasswordRequest workshopUserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        WorkshopUser currentWorkshopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("Workshop not found"));

        if(!passwordEncoder.matches(workshopUserRequest.getCurrentPassword(),currentWorkshopUser.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        if(passwordEncoder.matches(workshopUserRequest.getNewPassword(),currentWorkshopUser.getPassword())) {
            throw new IllegalArgumentException("New password and current password cannot be same");
        }
        currentWorkshopUser.setPassword(workshopUserRequest.getNewPassword());
        workshopUserRepository.save(currentWorkshopUser);
    }

    @Override
    public ResponseEntity<?> deleteAppUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopUser currentWorkshopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        workshopUserRepository.delete(currentWorkshopUser);
        return new ResponseEntity<>("AppUser Deleted successfully", HttpStatus.OK);
    }

    @Override
    public PersonalInfoResponse getPersonalProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new PersonalInfoResponse(currentWorkShopUser.getName(),
                currentWorkShopUser.getUsername(), currentWorkShopUser.getEmail(),
                Math.round(currentWorkShopUser.getRating() * 10.0) / 10.0);
    }

    @Override
    public void addWorkshopServiceType(WorkshopServiceTypeRequest workshopServiceType) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            WorkshopServiceType serviceType = parseWorkshopServiceType(workshopServiceType);
            if(currentWorkShopUser.getServicesOffered().contains(serviceType)) {
                log.warn("Workshop Service Type {} already exists for workshop {} ", serviceType.toString(),username);
                throw new RuntimeException("Workshop Service Type already exists");
            }
            currentWorkShopUser.getServicesOffered().add(serviceType);
            currentWorkShopUser.setAccountLastModifiedDateTime(LocalDateTime.now());
            updateWorkshopUserDetails(currentWorkShopUser);
            log.info("Added Workshop Service Type {}", serviceType.toString());
        }
        catch (Exception e) {
            log.error("Workshop Service cannot be added : {}", e.getMessage());
            throw new RuntimeException("Error occurred trying to save Workshop Service Details.");
        }
    }

    @Override
    public void addWorkshopVehicleType(WorkShopVehicleTypeRequest workshopVehicleType) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            VehicleType vehicleType = parseWorkshopVehicleType(workshopVehicleType);

            if (currentWorkShopUser.getVehicleTypeSupported() != null && currentWorkShopUser.getVehicleTypeSupported().equals(vehicleType)) {
                log.warn("Vehicle type {} already exists for workshop {}", vehicleType, username);
                throw new RuntimeException("Vehicle Type already exists");
            }
            currentWorkShopUser.setVehicleTypeSupported(vehicleType);
            currentWorkShopUser.setAccountLastModifiedDateTime(LocalDateTime.now());
            updateWorkshopUserDetails(currentWorkShopUser);
            log.info("Added Workshop Vehicle Type {}", vehicleType.toString());
        }
        catch (Exception e) {
            log.error("Workshop Vehicle Type cannot be added : {}", e.getMessage());
            throw new RuntimeException("Error occurred trying to save Workshop Vehicle Type.");
        }
    }

    private WorkshopServiceType parseWorkshopServiceType(WorkshopServiceTypeRequest workshopServiceType) {
        try {
            return WorkshopServiceType.valueOf(workshopServiceType.getWorkshopServiceType().toUpperCase());
        }
        catch (Exception e) {
            log.error("Invalid service type received : {}", e.getMessage());
            throw new RuntimeException("Invalid service type received." + workshopServiceType);
        }
    }

    private VehicleType parseWorkshopVehicleType(WorkShopVehicleTypeRequest workshopVehicleType) {
        try{
            return VehicleType.valueOf(workshopVehicleType.getWorkshopVehicleType().toUpperCase());
        }
        catch (Exception e) {
            log.error("Invalid vehicle type received : {}", e.getMessage());
            throw new RuntimeException("Invalid vehicle type received." + workshopVehicleType);
        }
    }

    @Override
    public void deleteWorkshopServiceType(WorkshopServiceTypeRequest workshopServiceTypeRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            WorkshopServiceType serviceType = parseWorkshopServiceType(workshopServiceTypeRequest);

            if(currentWorkShopUser.getServicesOffered() == null || username.isEmpty()) {
                log.warn("Workshop {} has no services added yet",username);
                throw new RuntimeException("No services added to remove from workshop");
            }
            if(!currentWorkShopUser.getServicesOffered().contains(serviceType)) {
                log.warn("Workshop {} does not currently offer service {}",username,serviceType.toString());
                throw new RuntimeException("Workshop does not offer this service");
            }
            currentWorkShopUser.getServicesOffered().remove(serviceType);
            currentWorkShopUser.setAccountLastModifiedDateTime(LocalDateTime.now());
            updateWorkshopUserDetails(currentWorkShopUser);
            log.info("Removed service {} from workshop {}", serviceType, username);

            currentWorkShopUser.getServicesOffered().remove(serviceType);
            updateWorkshopUserDetails(currentWorkShopUser);
            log.info("Deleted Workshop Service Type {}", serviceType.toString());
        } catch (Exception e) {
            log.error("Unexpected error while deleting workshop service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete workshop service");
        }
    }

    @Override
    public void deleteWorkshopVehicleType() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("Vehicle type removal requested by workshop [{}]", username);

        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentWorkShopUser.getVehicleTypeSupported() == null) {
            log.warn("Workshop [{}] has no vehicle type configured", username);
            throw new RuntimeException("No vehicle type configured");
        }

        currentWorkShopUser.setVehicleTypeSupported(null);
        currentWorkShopUser.setAccountLastModifiedDateTime(LocalDateTime.now());
        workshopUserRepository.save(currentWorkShopUser);

        log.info("All vehicle types removed for workshop [{}]", username);
    }

    @Override
    public List<WorkshopServiceType> getAllWorkshopServiceType() {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if(currentWorkShopUser.getServicesOffered() == null || currentWorkShopUser.getServicesOffered().isEmpty()) {
                log.info("Workshop {} has no 'servicesOffered' list initialized. Returning empty list.", username);
                return Collections.emptyList();
            }
            log.info("Successfully returned the list of services for workshop {}", username);
            return currentWorkShopUser.getServicesOffered();
        }
        catch (Exception e) {
            log.error("Unexpected error while fetching workshop services: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch workshop services");
        }
    }

    @Override
    public VehicleType getWorkshopSupportedVehicleType() {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            VehicleType workshopVehicleType = currentWorkShopUser.getVehicleTypeSupported();
            if(workshopVehicleType == null){
                log.info("Workshop {} has no supported vehicle type added yet returning null",username);
                return null;
            }
            log.info("Workshop {} supports service for service : {} ", username, workshopVehicleType);
            return workshopVehicleType;
        }
        catch (Exception e) {
            log.error("Unexpected error while fetching workshop vehicle type: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch workshop vehicle type");
        }
    }

    @Override
    public WorkshopStatusResponse getWorkshopCurrentStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new WorkshopStatusResponse(
                currentWorkShopUser.getId(),
                currentWorkShopUser.getName(),
                currentWorkShopUser.getUsername(),
                currentWorkShopUser.getCurrentWorkshopStatus(),
                currentWorkShopUser.getWorkshopAddress()
        );
    }

    @Override
    public WorkshopStatusResponse closeWorkshop() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Attempting to close workshop : {}",username);
        currentWorkShopUser.setCurrentWorkshopStatus(WorkshopStatus.CLOSED);
        workshopUserRepository.save(currentWorkShopUser);

        log.info("Workshop : {} , closed successfully",username);
        return new WorkshopStatusResponse(currentWorkShopUser.getId(), currentWorkShopUser.getName(),
                currentWorkShopUser.getUsername(),currentWorkShopUser.getCurrentWorkshopStatus(), currentWorkShopUser.getWorkshopAddress());
    }

    @Override
    public WorkshopAddressResponse getAddress() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        WorkshopUser currentWorkShopUser = workshopUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = currentWorkShopUser.getWorkshopAddress();
        if (address == null) {
            return new WorkshopAddressResponse(false, null);
        }
        AddressResponse response = new AddressResponse(
                address.getId(),
                address.getLatitude(),
                address.getLongitude(),
                address.getFormattedAddress(),
                address.isDefault()
        );
        return new WorkshopAddressResponse(true,response);
    }
}
