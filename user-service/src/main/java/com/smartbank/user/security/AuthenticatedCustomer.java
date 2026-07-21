package com.smartbank.user.security;

import com.smartbank.user.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class AuthenticatedCustomer {

    /**
     * @param callerCustomerId value of the gateway-injected {@code X-Customer-Id} header
     * @param targetId         the {@code {id}} path variable being accessed
     * @throws ForbiddenException if the header is absent or does not match the target
     */
    public void authorizeSelfAccess(String callerCustomerId, String targetId) {
        if (!StringUtils.hasText(callerCustomerId) || !callerCustomerId.equals(targetId)) {
            throw new ForbiddenException("You are not allowed to access this profile");
        }
    }
}
