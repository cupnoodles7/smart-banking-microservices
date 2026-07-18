package com.smartbank.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for creating a customer profile via {@code POST /users/internal} (PRD sec 6.7).
 *
 * <p>Bean Validation covers structural presence only; the '@'-in-email and 10-digit-phone
 * rules and uniqueness are enforced in the service layer (PRD sec 7.2).
 */
public class CreateUserRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    @NotNull(message = "address is required")
    @Valid
    private AddressDto address;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }
}
