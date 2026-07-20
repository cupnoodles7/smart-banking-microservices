package com.smartbank.auth.client;

import com.smartbank.auth.dto.request.AddressRequest;

/**
 * Body sent to user-service's {@code POST /users/internal}. Field names match
 * user-service's {@code CreateUserRequest} so Jackson maps them one-to-one. The
 * {@code id} is the customerId Auth generates, which becomes the profile's {@code _id}.
 */
public class ProfileCreateRequest {

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private AddressRequest address;

    public ProfileCreateRequest() {
    }

    public ProfileCreateRequest(String id, String fullName, String email,
                                String phoneNumber, AddressRequest address) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public AddressRequest getAddress() {
        return address;
    }

    public void setAddress(AddressRequest address) {
        this.address = address;
    }
}
