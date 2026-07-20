package com.smartbank.user.service.impl;

import com.smartbank.user.dto.request.CreateUserRequest;
import com.smartbank.user.dto.request.UpdateUserRequest;
import com.smartbank.user.dto.response.UserResponse;
import com.smartbank.user.entity.User;
import com.smartbank.user.exception.DuplicateCustomerException;
import com.smartbank.user.exception.InvalidEmailException;
import com.smartbank.user.exception.InvalidPhoneNumberException;
import com.smartbank.user.exception.UserNotFoundException;
import com.smartbank.user.mapper.UserMapper;
import com.smartbank.user.repository.UserRepository;
import com.smartbank.user.service.UserService;
import com.smartbank.user.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Profile use cases. Discipline: validate first, then mutate (PRD sec 7.2).
 * Logs INFO on successful create/update, WARN on validation failures; DB failures
 * surface to {@code GlobalExceptionHandler}, which logs them at ERROR (PRD sec 6.13).
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse getById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user found with id " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        validateFormat(request.getEmail(), request.getPhoneNumber());

        // Auth supplies the id (== customerId); reject a collision rather than silently
        // overwriting an existing profile via save().
        if (userRepository.existsById(request.getId())) {
            log.warn("Rejected create: id already in use ({})", request.getId());
            throw new DuplicateCustomerException("Customer id already in use: " + request.getId());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Rejected create: email already in use ({})", request.getEmail());
            throw new DuplicateCustomerException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Rejected create: phone already in use ({})", request.getPhoneNumber());
            throw new DuplicateCustomerException("Phone number already in use: " + request.getPhoneNumber());
        }

        User saved = userRepository.save(userMapper.toEntity(request));
        log.info("Created user profile id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user found with id " + id));

        validateFormat(request.getEmail(), request.getPhoneNumber());

        // Only enforce uniqueness against *other* documents when the value actually changes.
        if (!request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Rejected update for id={}: email already in use ({})", id, request.getEmail());
            throw new DuplicateCustomerException("Email already in use: " + request.getEmail());
        }
        if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Rejected update for id={}: phone already in use ({})", id, request.getPhoneNumber());
            throw new DuplicateCustomerException("Phone number already in use: " + request.getPhoneNumber());
        }

        userMapper.applyUpdate(user, request);
        User saved = userRepository.save(user);
        log.info("Updated user profile id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    /** PRD sec 7.2: email must contain '@', phone must be exactly 10 digits. */
    private void validateFormat(String email, String phoneNumber) {
        if (!ValidationUtils.isValidEmail(email)) {
            log.warn("Rejected write: invalid email format ({})", email);
            throw new InvalidEmailException("Email must contain '@': " + email);
        }
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            log.warn("Rejected write: invalid phone format ({})", phoneNumber);
            throw new InvalidPhoneNumberException("Phone number must be exactly 10 digits: " + phoneNumber);
        }
    }
}
