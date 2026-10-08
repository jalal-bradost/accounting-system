package com.bradox.erp.sign.service.domain.dto;

import java.util.UUID;

/** A personal link, shown once. Never retrievable later (BR-SIG-03). */
public record SignerLinkResponse(UUID signerId, String name, String roleName, String url) {
}
