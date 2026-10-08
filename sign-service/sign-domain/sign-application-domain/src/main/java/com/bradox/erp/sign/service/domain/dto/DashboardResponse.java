package com.bradox.erp.sign.service.domain.dto;

public record DashboardResponse(long waitingForMe, long waitingForOthers, long completedThisMonth, long expiringSoon,
                                long needsAttention, long reminderDue) {
}
