package com.smartbank.wallet.exception;

import com.smartbank.wallet.constants.FailureReason;
import lombok.Getter;

/**
 * Raised internally when a business rule is violated (insufficient balance, wallet
 * limit, daily limit, invalid amount, self-transfer).
 *
 * <p>Per PRD §7.3 this NEVER crosses the REST boundary: the service catches it,
 * records a FAILED transaction, and returns HTTP 200 with the carried
 * {@link FailureReason}. It exists so validation can follow the "validate first,
 * then mutate; throw to raise" discipline (PRD §7.2) while still honouring the
 * Track B "no exceptions over REST for business rules" rule.
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final FailureReason failureReason;

    public BusinessRuleException(FailureReason failureReason, String message) {
        super(message);
        this.failureReason = failureReason;
    }
}
