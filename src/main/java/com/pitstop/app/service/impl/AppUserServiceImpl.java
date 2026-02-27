package com.pitstop.app.service.impl;

import com.pitstop.app.constants.VehicleType;
import com.pitstop.app.constants.WorkshopServiceType;
import com.pitstop.app.dto.*;
import com.pitstop.app.exception.UserAlreadyExistException;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.model.*;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.repository.PricingRuleRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import com.pitstop.app.service.AppUserService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserServiceImpl implements AppUserService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager manager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final PricingRuleRepository pricingRuleRepository;
    private final WorkshopUserRepository workshopUserRepository;

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
    public AppUserRegisterResponse saveAppUserDetails(AppUserRegisterRequest appUserRequest) {
        appUserRequest.setUsername("user_"+appUserRequest.getUsername());

        //register new AppUsers
        boolean emailExists = appUserRepository.findByEmail(appUserRequest.getEmail()).isPresent();
        boolean usernameExists = appUserRepository.findByUsername(appUserRequest.getUsername()).isPresent();

        if(emailExists || usernameExists){
            throw new UserAlreadyExistException("AppUser already exists");
        }
        AppUser appUser = new AppUser();
        appUser.setName(appUserRequest.getName());
        appUser.setUsername(appUserRequest.getUsername());
        appUser.setEmail(appUserRequest.getEmail());
        appUser.setPassword(passwordEncoder.encode(appUserRequest.getPassword()));
        appUser.setAccountCreationDateTime(LocalDateTime.now());
        appUserRepository.save(appUser);

        AppUserRegisterResponse response = new AppUserRegisterResponse();
        response.setId(appUser.getId());
        response.setName(appUserRequest.getName());
        response.setUsername(appUserRequest.getUsername());
        response.setEmail(appUserRequest.getEmail());
        response.setMessage("AppUser account created successfully");
        return response;
    }
    public void updateAppUserDetails(AppUser appUser){
        if(appUser.getId() != null && appUserRepository.existsById(appUser.getId())){
            appUserRepository.save(appUser);
        }
        else {
            appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
            appUserRepository.save(appUser);
        }
    }

    @Override
    public AppUser getAppUserById(String id) {
        return appUserRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("AppUser not found with ID :"+id));
    }

    @Override
    public AppUser getAppUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException("AppUser not found with username :"+username));
    }

    @Override
    public List<AppUser> getAllAppUser() {
        return new ArrayList<>(appUserRepository.findAll());
    }

    @Override
    @Transactional
    public String addAddress(AddressRequest address) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Address finalAddress = null;

        // case 1 : if user provides coordinates (gps location)
        if(address.getLatitude() != null && address.getLongitude() != null){
            AddressResponse addressResponse = findAddressFromCoordinates(address.getLatitude(),address.getLongitude());
            finalAddress = Address.builder()
                    .latitude(address.getLatitude())
                    .longitude(address.getLongitude())
                    .formattedAddress(addressResponse.getFormattedAddress())
                    .build();
        }

        //case 2 : if user provides plain text address

        if (address.getFormattedAddress() != null){
            AddressResponse addressResponse = findCoordinatesFromAddress(address.getFormattedAddress());
            finalAddress = Address.builder()
                    .latitude(addressResponse.getLatitude())
                    .longitude(addressResponse.getLongitude())
                    .formattedAddress(address.getFormattedAddress())
                    .build();
        }
        if(finalAddress != null){
            if(appUser.getUserAddress().isEmpty()){
                finalAddress.setDefault(true);
            } else {
                finalAddress.setDefault(false);
            }
        }
        appUser.getUserAddress().add(finalAddress);
        appUser.setAccountLastModifiedDateTime(LocalDateTime.now());
        updateAppUserDetails(appUser);
        return "Address added successfully";
    }

    @Override
    public String changeDefaultAddress(AddressRequest addressRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Address> addresses = appUser.getUserAddress();
        if (addresses == null) addresses = new ArrayList<>();

        // Mark all as non-default
        addresses.forEach(addr -> addr.setDefault(false));

        //Check if the address already exists
        Optional<Address> existingAddressOpt = addresses.stream()
                .filter(addr -> addr.getLatitude().equals(addressRequest.getLatitude())
                        && addr.getLongitude().equals(addressRequest.getLongitude()))
                .findFirst();

        Address newDefault;

        if (existingAddressOpt.isPresent()) {
            // Existing address becomes default
            newDefault = existingAddressOpt.get();
            newDefault.setDefault(true);
            // Remove and reinsert at index 0
            addresses.remove(newDefault);
            addresses.add(0, newDefault);
        } else {
            // Step 3: Create new address and set as default
            AddressResponse addressResponse = findAddressFromCoordinates(
                    addressRequest.getLatitude(),
                    addressRequest.getLongitude()
            );

            newDefault = Address.builder()
                    .latitude(addressRequest.getLatitude())
                    .longitude(addressRequest.getLongitude())
                    .formattedAddress(addressResponse.getFormattedAddress())
                    .isDefault(true)
                    .build();

            // Add at the top of the list
            addresses.add(0, newDefault);
        }

        // Step 4: Save and update timestamp
        appUser.setUserAddress(addresses);
        appUser.setAccountLastModifiedDateTime(LocalDateTime.now());
        appUserRepository.save(appUser);

        return "Default address updated successfully";
    }
    public AppUserLoginResponse loginAppUser(AppUserLoginRequest req){
        req.setUsername("user_" + req.getUsername());
        log.info("Login attempt for AppUser: {}", req.getUsername());
        try {
            manager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Invalid credentials for AppUser: {}", req.getUsername());
            throw new RuntimeException("Incorrect username or password");
        }

        CustomUserDetails user =
                (CustomUserDetails) userDetailsService.loadUserByUsername(req.getUsername());

        if (user.getUserType() != UserType.APP_USER) {
            log.warn("User {} attempted AppUser login but is {}",
                    req.getUsername(), user.getUserType());
            throw new RuntimeException("Only App Users can login here");
        }

        String token = jwtUtil.generateToken(user);

        log.info("AppUser {} logged in successfully", req.getUsername());
        System.out.println(user.getUsername().substring(user.getUsername().indexOf('_') + 1));
        return new AppUserLoginResponse(
                user.getUsername().substring(user.getUsername().indexOf('_') + 1),
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

            HttpRequest request = HttpRequest.newBuilder()
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
    public String updateAppUser(AppUserRequest appUserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser currentAppUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(appUserRequest.getName() != null) {
            currentAppUser.setName(appUserRequest.getName());
        }
        if(appUserRequest.getEmail() != null) {
            currentAppUser.setEmail(appUserRequest.getEmail());
        }
        if(appUserRequest.getUsername() != null) {
            currentAppUser.setUsername(appUserRequest.getUsername());
        }

        updateAppUserDetails(currentAppUser);

        return "AppUser Details updated successfully";
    }
    @Override
    public AppUserResponse getAppUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser currentAppUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AppUserResponse appUserResponse = new AppUserResponse();
        appUserResponse.setName(currentAppUser.getName());
        appUserResponse.setEmail(currentAppUser.getEmail());
        appUserResponse.setAddresses(currentAppUser.getUserAddress());

        return appUserResponse;
    }
    @Override
    public ResponseEntity<?> changePassword(AppUserRequest appUserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser currentAppUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String oldPassword = currentAppUser.getPassword();
        String newPassword = appUserRequest.getPassword();
        if(newPassword.equals(oldPassword)) {
            return new ResponseEntity<>("Change Password cannot be same", HttpStatusCode.valueOf(500));
        }

        currentAppUser.setPassword(newPassword);
        currentAppUser.setAccountLastModifiedDateTime(LocalDateTime.now());
        updateAppUserDetails(currentAppUser);
        return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> deleteAppUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser currentAppUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        appUserRepository.delete(currentAppUser);
        return new ResponseEntity<>("AppUser Deleted successfully", HttpStatus.OK);
    }

    @Override
    public PersonalInfoResponse getPersonalProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AppUser currentAppUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new PersonalInfoResponse(currentAppUser.getName(),
                currentAppUser.getUsername(), currentAppUser.getEmail(),
                Math.round(currentAppUser.getRating() * 10.0) / 10.0);
    }

    @Override
    public GetPriceResponse getPrice(GetPriceRequest request) {
        try {
            log.info("Price request received | vehicle={} service={} workshopId={}",
                    request.getVehicleType(),
                    request.getServiceType(),
                    request.getWorkshopId());
            validateRequest(request);
            VehicleType vt = parseWorkshopVehicleType(request.getVehicleType());
            WorkshopServiceType st = parseWorkshopServiceType(request.getServiceType());
            PricingRule pricingRule = pricingRuleRepository.findByVehicleTypeAndServiceType(vt, st)
                    .orElseThrow(() -> {
                        log.error("Pricing rule not defined for vehicle type {} and service type {}", vt, st);
                        return new ResourceNotFoundException("Pricing rule not defined for vehicle type " + vt + " and service type " + st);
                    });
            double baseAmount = pricingRule.getAmount();
            if (request.getWorkshopId() == null) {
                log.info("Returning Estimated Price");
                return GetPriceResponse.builder()
                        .baseAmount(baseAmount)
                        .premiumAmount(pricingRule.getPremiumAmount())
                        .finalAmount(baseAmount)
                        .premiumApplied(false)
                        .message("Estimated Base Price")
                        .build();
            }
            WorkshopUser workshopUser = workshopUserRepository.findById(request.getWorkshopId())
                    .orElseThrow(() -> {
                        log.error("Workshop User not found for workshopId {}", request.getWorkshopId());
                        return new ResourceNotFoundException("Workshop User not found for workshopId " + request.getWorkshopId());
                    });
            validateWorkshopSupport(workshopUser, request);

            double premiumAmount = 0;
            boolean premiumApplied = false;
            if (workshopUser.isPremiumWorkshop()) {
                premiumAmount = pricingRule.getPremiumAmount();
                premiumApplied = true;
            }
            double finalAmount = baseAmount + premiumAmount;
            log.info("Final price calculated | base={} premium={} final={}",
                    baseAmount, premiumAmount, finalAmount);

            return GetPriceResponse.builder()
                    .baseAmount(baseAmount)
                    .premiumAmount(premiumAmount)
                    .finalAmount(finalAmount)
                    .premiumApplied(premiumApplied)
                    .message(premiumApplied ?
                            "Premium workshop pricing applied"
                            : "Standard workshop pricing applied")
                    .build();
        }
        catch (Exception e) {
            log.error("Error while fetching price");
            throw new RuntimeException("Error while fetching price" + e.getMessage());
        }
    }
    private VehicleType parseWorkshopVehicleType(String workshopVehicleType) {
        try{
            return VehicleType.valueOf(workshopVehicleType.toUpperCase());
        }
        catch(Exception e){
            log.error("Invalid vehicle type: {}", workshopVehicleType, e);
            throw new RuntimeException("Invalid vehicle type: " + workshopVehicleType);
        }
    }
    private WorkshopServiceType parseWorkshopServiceType(String workshopServiceType) {
        try{
            return WorkshopServiceType.valueOf(workshopServiceType.toUpperCase());
        }
        catch(Exception e){
            log.error("Invalid service type: {}", workshopServiceType, e);
            throw new RuntimeException("Invalid service type: " + workshopServiceType);
        }
    }
    private void validateRequest(GetPriceRequest request) {
        if (request.getVehicleType() == null || request.getServiceType() == null) {
            throw new IllegalArgumentException("Vehicle type and service type are required");
        }
    }

    private void validateWorkshopSupport(WorkshopUser workshop, GetPriceRequest request) {
        WorkshopServiceType st =  parseWorkshopServiceType(request.getServiceType());
        if (!workshop.getServicesOffered().contains(st)) {
            log.error("Workshop service type {} does not exist", st);
            throw new RuntimeException("Workshop does not support this service");
        }
        VehicleType vt = parseWorkshopVehicleType(request.getVehicleType());
        if (!workshop.getVehicleTypeSupported().equals(vt)) {
            log.error("Workshop does not support this vehicle type {}", vt);
            throw new RuntimeException("Workshop does not support this vehicle type");
        }
    }
}
