package com.smartbank.user.service;

import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;


public interface UserService {

    UserResponse getById(String id);

    UserResponse updateUser(String id, UpdateUserRequest request);

    UserResponse createUser(CreateUserRequest request);

    void deleteById(String id);
}
