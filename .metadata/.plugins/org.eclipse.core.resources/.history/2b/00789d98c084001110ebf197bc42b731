package com.smartbank.user.constants;

/**
 * Shared, cross-cutting constants for the User Service: the trusted identity headers
 * injected by the API Gateway (PRD sec 6.2 / 6.8) and the profile validation rules
 * (PRD sec 7.2).
 */
public final class UserServiceConstants {

    private UserServiceConstants() {
    }

    /**
     * Authenticated customer id, forwarded by the Gateway after it validates the JWT.
     * Downstream services trust this header rather than re-parsing the token.
     */
    public static final String HEADER_CUSTOMER_ID = "X-Customer-Id";

    /**
     * Shared secret guarding the internal, service-to-service profile-create endpoint.
     * Placeholder scheme until a dedicated internal-auth convention exists repo-wide.
     */
    public static final String HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key";

    /** A phone number must be exactly this many digits (PRD sec 7.2). */
    public static final int PHONE_NUMBER_LENGTH = 10;
}
