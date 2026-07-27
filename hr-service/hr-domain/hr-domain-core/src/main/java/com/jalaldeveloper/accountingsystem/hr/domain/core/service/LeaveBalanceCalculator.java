package com.jalaldeveloper.accountingsystem.hr.domain.core.service;

import java.math.BigDecimal;
import java.util.List;

public class LeaveBalanceCalculator {

    public record AllocationSlice(
            String timeOffTypeId,
            BigDecimal allocatedDays,
            BigDecimal usedDays) {}

    public record RequestSlice(
            String state,
            BigDecimal numberOfDays) {}

    public record BalanceSummary(
            BigDecimal allocatedDays,
            BigDecimal usedDays,
            BigDecimal availableDays,
            BigDecimal pendingDays) {

        public static BalanceSummary from(List<AllocationSlice> allocations, List<RequestSlice> requests) {
            BigDecimal allocated = allocations.stream()
                    .map(AllocationSlice::allocatedDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal used = allocations.stream()
                    .map(AllocationSlice::usedDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pending = requests.stream()
                    .filter(r -> "confirm".equals(r.state()))
                    .map(RequestSlice::numberOfDays)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal available = allocated.subtract(used).subtract(pending);
            return new BalanceSummary(allocated, used, available, pending);
        }
    }
}
