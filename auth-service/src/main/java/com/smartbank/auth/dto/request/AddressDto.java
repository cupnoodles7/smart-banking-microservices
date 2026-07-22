package com.smartbank.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//Postal address collected at registration and forwarded to the User Service when the customer profile is created
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
