package com.smartbank.user.security;

import com.smartbank.user.exception.ForbiddenException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Ownership check for the JWT-protected profile endpoints ({@code GET}/{@code PUT}
 * {@code /users/{id}}).
 *
 * <p>Reuses the platform's existing identity convention rather than re-parsing tokens:
 * the API Gateway validates the JWT and forwards the caller's id in the trusted
 * {@code X-Customer-Id} header (PRD sec 6.2 / 6.8). Because the User document {@code _id}
 * is the system-wide {@code customerId}, a caller may only read/update their own profile
 * when that header equals the path {@code {id}}.
 */
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
