package com.smartbank.account.util;

import com.smartbank.account.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

// Lazy reset strategy (PRD sec 6.9): daily counters are zeroed on the first
// transaction of a new day instead of via a scheduled job.
//
// if (lastLimitResetDate != today) {
//     todayTransactionCount = 0;
//     dailyTransferredAmount = 0;
//     lastLimitResetDate = today;
// }
public final class DailyLimitResetEvaluator {

    private DailyLimitResetEvaluator() {
    }

    // Mutates the account in place if its counters belong to a previous day.
    public static void resetIfNewDay(Account account) {
        LocalDate today = LocalDate.now();
        if (!today.equals(account.getLastLimitResetDate())) {
            account.setTodayTransactionCount(0);
            account.setDailyTransferredAmount(BigDecimal.ZERO);
            account.setLastLimitResetDate(today);
        }
    }
}
