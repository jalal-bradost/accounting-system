package com.bradox.erp.sign.service.domain.dto;

import java.util.List;

public record SendResultResponse(RequestResponse request, List<SignerLinkResponse> links) {
}
