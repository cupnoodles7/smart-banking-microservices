package com.smartbank.user.security;

import com.smartbank.user.exception.ForbiddenException;
import org.springframework.util.StringUtils;


public final class AuthenticatedCustomer {

    private AuthenticatedCustomer() {
    }

    public static void authorizeSelfAccess(String callerCustomerId, String targetId) {
        if (!StringUtils.hasText(callerCustomerId) || !callerCustomerId.equals(targetId)) {
            throw new ForbiddenException("You are not allowed to access this profile");
        }
    }
}
