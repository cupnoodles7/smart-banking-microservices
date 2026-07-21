package com.smartbank.user.service;

import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;


public interface UserService {

    /** Fetch a profile by id, or throw {@code UserNotFoundException}. */
    UserResponse getById(String id);

    /** Update a profile after validating field format and uniqueness. */
    UserResponse updateUser(String id, UpdateUserRequest request);

    /** Create a profile (called service-to-service during registration). */
    UserResponse createUser(CreateUserRequest request);
}
