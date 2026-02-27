package com.pitstop.app.service.impl;

import com.pitstop.app.dto.*;
import com.pitstop.app.exception.ResourceNotFoundException;
import com.pitstop.app.exception.UserAlreadyExistException;
import com.pitstop.app.model.*;
import com.pitstop.app.repository.AdminUserRepository;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.repository.BookingRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import com.pitstop.app.service.AdminUserService;
import com.pitstop.app.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {
    private final AppUserRepository appUserRepository;
    private final WorkshopUserRepository workshopUserRepository;
    private final AdminUserRepository adminUserRepository;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthenticationManager manager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final BookingServiceImpl bookingService;
    private final BookingRepository bookingRepository;

    @Transactional
    public String changeUserRole(String username, String newRole) {
        BaseUser user = appUserRepository.findByUsername(username)
                .map(u -> (BaseUser) u)
                .orElseGet(() -> workshopUserRepository.findByUsername(username)
                        .map(u -> (BaseUser) u)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        return updateUserRole(user, newRole);
    }

    @Override
    public List<Booking> getBookingHistoryAppUser(String appUserId) {
        try {
            return bookingRepository.findByAppUserIdOrderByBookingStartedTimeDesc(appUserId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Booking> getBookingHistoryWorkShopUser(String workshopUserId) {
        try {
            return bookingRepository.findByWorkshopUserIdOrderByBookingStartedTimeDesc(workshopUserId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Booking getBookingDetailsById(String bookingId) {
        try {
            return bookingService.getBookingById(bookingId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkshopUserResponse setPremium(String workshopUserId) {
        log.info("Setting premium for workshop with id {}", workshopUserId);
        WorkshopUser workshopUser = workshopUserRepository.findById(workshopUserId)
                .orElseThrow(() -> {
                    log.error("Workshop user not found with id {}", workshopUserId);
                    return new ResourceNotFoundException("Workshop user not found with id" + workshopUserId);
                });
        workshopUser.setPremiumWorkshop(true);
        workshopUser.setAccountLastModifiedDateTime(LocalDateTime.now());
        workshopUserRepository.save(workshopUser);
        WorkshopUserResponse response = new WorkshopUserResponse();
        response.setIsPremium(workshopUser.isPremiumWorkshop());
        return response;
    }

    private String updateUserRole(BaseUser user, String newRole) {
        user.setRoles(List.of(newRole.toUpperCase()));

        if (user instanceof AppUser appUser) {
            appUserRepository.save(appUser);
        } else if (user instanceof WorkshopUser workshopUser) {
            workshopUserRepository.save(workshopUser);
        }

        return "Role updated for: " + user.getUsername();
    }
    public AdminUserRegisterResponse createAdmin(AdminUserRegisterRequest request) {
        try {
            request.setUsername("admin_"+request.getUsername());
            log.info("Creating a new admin user :: "+request.getUsername());
            boolean isEmailExists = adminUserRepository.findByEmail(request.getEmail()).isPresent();
            boolean isUsernameExists = adminUserRepository.findByUsername(request.getUsername()).isPresent();

            if (isEmailExists || isUsernameExists) {
                log.error("Email {} or Username {} already exists.", request.getEmail(), request.getUsername());
                throw new UserAlreadyExistException("AdminUser already exists");
            }
            AdminUser adminUser = new AdminUser();
            adminUser.setName(request.getName());
            adminUser.setUsername(request.getUsername());
            adminUser.setEmail(request.getEmail());
            adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
            adminUser.setAccountCreationDateTime(LocalDateTime.now());
            adminUser.setAccountLastModifiedDateTime(LocalDateTime.now());
            adminUserRepository.save(adminUser);
            log.info("Admin user created");

            return AdminUserRegisterResponse.builder()
                    .id(adminUser.getId())
                    .name(adminUser.getName())
                    .username(adminUser.getUsername())
                    .email(adminUser.getEmail())
                    .createdAt(adminUser.getAccountCreationDateTime())
                    .message("New Admin User created successfully")
                    .build();
        }
        catch (Exception e) {
            throw new RuntimeException("Error while creating AdminUser", e);
        }
    }
    public AdminUserLoginResponse loginAdminUser(AdminUserLoginRequest req){
        req.setUsername("admin_"+req.getUsername());
        log.info("Login attempt for Admin: {}", req.getUsername());

        // Step 1: Authenticate username/password
        try {
            manager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Invalid Admin credentials for {}", req.getUsername());
            throw new RuntimeException("Incorrect username or password");
        }

        // Step 2: Load full user details
        CustomUserDetails user =
                (CustomUserDetails) userDetailsService.loadUserByUsername(req.getUsername());

        // Step 3: Enforce ADMIN-only login rule
        if (user.getUserType() != UserType.ADMIN) {
            log.warn("User {} attempted Admin login but is {}",
                    req.getUsername(), user.getUserType());
            throw new RuntimeException("Only Admins can login here");
        }

        // Step 4: Generate full JWT
        String token = jwtUtil.generateToken(user);

        log.info("Admin {} logged in successfully", req.getUsername());

        return new AdminUserLoginResponse(
                user.getUsername().substring(user.getUsername().indexOf('_') + 1),
                token,
                "Login successful"
        );
    }
}
