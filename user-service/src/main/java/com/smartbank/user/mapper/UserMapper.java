package com.smartbank.user.mapper;

import com.smartbank.user.dto.request.AddressDto;
import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;
import com.smartbank.user.entity.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {

    // Build a new entity from a create request. Timestamps are set by auditing.
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        // Auth supplies the id so the document _id == the system-wide customerId.
        user.setId(request.getId());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(toAddressEntity(request.getAddress()));
        return user;
    }

    // Apply an update request onto an existing, already-loaded entity in place. 
    public void applyUpdate(User user, UpdateUserRequest request) {
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(toAddressEntity(request.getAddress()));
    }

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(toAddressDto(user.getAddress()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private User.Address toAddressEntity(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        User.Address address = new User.Address();
        address.setLine1(dto.getLine1());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        return address;
    }

    private AddressDto toAddressDto(User.Address address) {
        if (address == null) {
            return null;
        }
        AddressDto dto = new AddressDto();
        dto.setLine1(address.getLine1());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPincode(address.getPincode());
        return dto;
    }
}
