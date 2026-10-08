package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/**
 * One signer of a request. {@code roleKey} is the template role id (or any key for a one-off request, matching the
 * {@code roleKey} of the fields). Give a contact or user to link them, or just a name and email.
 */
public record SignerInput(String roleKey, String roleName, boolean approverOnly, UUID partnerId, UUID userId, String name,
                          String email, String phone, String channel) {
}
