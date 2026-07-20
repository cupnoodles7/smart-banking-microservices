package com.smartbank.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

// Postal address collected at registration and forwarded to user-service when the
// customer profile is created. Field names mirror user-service's AddressDto so the
// JSON body maps straight onto it.
public class AddressRequest {

    @NotBlank(message = "address.line1 is required")
    private String line1;

    @NotBlank(message = "address.city is required")
    private String city;

    @NotBlank(message = "address.state is required")
    private String state;

    @NotBlank(message = "address.pincode is required")
    private String pincode;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}
