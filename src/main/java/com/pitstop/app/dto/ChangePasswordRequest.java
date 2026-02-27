package com.pitstop.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Current password cannot be blank")
    private String currentPassword;
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
}
