package com.smartbank.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDto {

    @NotBlank(message = "address.line1 is required")
    private String line1;

    @NotBlank(message = "address.city is required")
    private String city;

    @NotBlank(message = "address.state is required")
    private String state;

    @NotBlank(message = "address.pincode is required")
    private String pincode;
}
