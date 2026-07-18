package com.smartbank.user.mapper;

import com.smartbank.user.dto.request.AddressDto;
import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.AddressResponse;
import com.smartbank.user.dto.response.UserResponse;
import com.smartbank.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Converts between DTOs and the {@link User} entity so controllers never touch the
 * entity directly: Request DTO -> Entity -> Response DTO (PRD sec 6.10).
 */
@Component
public class UserMapper {

    /** Build a new entity from a create request. Timestamps are set by auditing. */
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(toAddressEntity(request.getAddress()));
        return user;
    }

    /** Apply an update request onto an existing, already-loaded entity in place. */
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
        response.setAddress(toAddressResponse(user.getAddress()));
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

    private AddressResponse toAddressResponse(User.Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setLine1(address.getLine1());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setPincode(address.getPincode());
        return response;
    }
}
