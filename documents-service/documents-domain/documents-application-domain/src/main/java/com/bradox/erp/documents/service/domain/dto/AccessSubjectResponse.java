package com.bradox.erp.documents.service.domain.dto;

import java.util.UUID;

/** A user or role that can be picked in the folder access dialog. */
public record AccessSubjectResponse(String type, UUID id, String name) {
}
