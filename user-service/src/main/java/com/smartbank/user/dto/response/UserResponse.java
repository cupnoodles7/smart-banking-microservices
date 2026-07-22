package com.smartbank.user.dto.response;

import com.smartbank.user.dto.request.AddressDto;

import java.time.Instant;

import lombok.Data;

@Data
public class UserResponse {

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private AddressDto address;
    private Instant createdAt;
    private Instant updatedAt;
}
