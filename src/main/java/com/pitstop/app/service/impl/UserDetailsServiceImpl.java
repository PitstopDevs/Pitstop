package com.pitstop.app.service.impl;

import com.pitstop.app.model.*;
import com.pitstop.app.repository.AdminUserRepository;
import com.pitstop.app.repository.AppUserRepository;
import com.pitstop.app.repository.WorkshopUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final WorkshopUserRepository workshopUserRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Attempting login for username: {}", username);

        int indexOfFirstUnderScore = username.indexOf('_');
        String role = username.substring(0, indexOfFirstUnderScore + 1);

        if(role.equals("workshop_")) {
            // Try WorkshopUser
            WorkshopUser workshopUser = workshopUserRepository.findByUsername(username).orElse(null);
            if (workshopUser != null) {
                log.info("Found WorkshopUser: {} with roles={}", workshopUser.getUsername(), workshopUser.getRoles());
                return new CustomUserDetails(workshopUser, workshopUser.getUserType());
            }
        } else if(role.equals("user_")) {
            // Try AppUser
            AppUser appUser = appUserRepository.findByUsername(username).orElse(null);
            if (appUser != null) {
                log.info("Found AppUser: {} with roles={}", appUser.getUsername(), appUser.getRoles());
                return new CustomUserDetails(appUser, appUser.getUserType());
            }
        } else if(role.equals("admin_")){
            // Try AdminUser
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElse(null);
            if (adminUser != null) {
                log.info("Found AdminUser: {} with roles={}", adminUser.getUsername(), adminUser.getRoles());
                return new CustomUserDetails(adminUser, adminUser.getUserType());
            }
        }
        log.warn("Username {} not found in any user collection", username);
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
