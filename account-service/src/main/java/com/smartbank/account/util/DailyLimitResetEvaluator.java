package com.smartbank.account.util;

import com.smartbank.account.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
public final class DailyLimitResetEvaluator {

    private DailyLimitResetEvaluator() {
    }
    public static void resetIfNewDay(Account account) {
        LocalDate today = LocalDate.now();
        if (!today.equals(account.getLastLimitResetDate())) {
            account.setTodayTransactionCount(0);
            account.setDailyTransferredAmount(BigDecimal.ZERO);
            account.setLastLimitResetDate(today);
        }
    }
}
