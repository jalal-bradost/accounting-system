package com.bradox.erp.timesheet.domain.core.valueobject;

/** SKIPPED means the week had no cost to post (TSH-10 #4). Only a REVERSED posting stops counting as active. */
public enum PostingStatus {
    PENDING, POSTED, FAILED, REVERSED, SKIPPED
}
