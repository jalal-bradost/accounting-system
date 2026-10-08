package com.bradox.erp.sign.service.domain.dto;

/** A ready-made message to copy, in the three languages, with the link for the person to remind. */
public record ReminderTextResponse(String url, String english, String arabic, String kurdish) {
}
