package com.smartbank.user.security;

import com.smartbank.user.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class AuthenticatedCustomer {

    public void authorizeSelfAccess(String callerCustomerId, String targetId) {
        if (!StringUtils.hasText(callerCustomerId) || !callerCustomerId.equals(targetId)) {
            throw new ForbiddenException("You are not allowed to access this profile");
        }
    }
}
