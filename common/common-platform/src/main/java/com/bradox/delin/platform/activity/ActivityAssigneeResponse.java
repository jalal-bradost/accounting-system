package com.bradox.delin.platform.activity;

/**
 * Lightweight user option for scheduling activities.
 * {@code id} is the stable assignee key (username) stored on {@link ActivityMessage#getAssigneeId()}.
 */
public record ActivityAssigneeResponse(String id, String username, String displayName) {}
