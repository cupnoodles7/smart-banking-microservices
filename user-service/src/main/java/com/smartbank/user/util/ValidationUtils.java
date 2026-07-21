package com.smartbank.user.util;

import com.smartbank.user.constants.UserServiceConstants;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null
                && phoneNumber.length() == UserServiceConstants.PHONE_NUMBER_LENGTH
                && phoneNumber.chars().allMatch(Character::isDigit);
    }
}
