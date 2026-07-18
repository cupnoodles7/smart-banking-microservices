package com.smartbank.user.util;

import com.smartbank.user.constants.UserServiceConstants;

/**
 * Stateless helpers for the two profile-field rules the User Service enforces before
 * any DB write (PRD sec 7.2): email must contain '@', phone must be exactly 10 digits.
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /** PRD sec 7.2: email is invalid when it is missing '@'. */
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    /** PRD sec 7.2: phone must be exactly 10 digits. */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null
                && phoneNumber.length() == UserServiceConstants.PHONE_NUMBER_LENGTH
                && phoneNumber.chars().allMatch(Character::isDigit);
    }
}
