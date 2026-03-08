package com.jalaldeveloper.accountingsystem.application.rest;

/**
 * Optional request body for POST /journal-entries/{id}/reverse.
 * If omitted, reason defaults to "Reversal".
 */
public class ReverseJournalEntryRequest {
    private String reason;

    public ReverseJournalEntryRequest() {}
    public ReverseJournalEntryRequest(String reason) { this.reason = reason; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
