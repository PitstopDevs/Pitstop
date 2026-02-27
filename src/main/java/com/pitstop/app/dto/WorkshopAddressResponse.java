package com.pitstop.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkshopAddressResponse {
    private boolean hasAddress;
    private AddressResponse address;
}
