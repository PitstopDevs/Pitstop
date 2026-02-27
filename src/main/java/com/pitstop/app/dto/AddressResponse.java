package com.pitstop.app.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {
    private String id;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private boolean isDefault;
}
