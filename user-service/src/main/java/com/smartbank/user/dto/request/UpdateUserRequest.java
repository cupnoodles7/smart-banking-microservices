package com.smartbank.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "\\d{10}", message = "phoneNumber must be exactly 10 digits")
    private String phoneNumber;

    @NotNull(message = "address is required")
    @Valid
    private AddressDto address;
}
