package com.bradox.erp.sign.service.domain.dto;

import java.util.List;

public record PublicSubmitCommand(boolean consent, List<PublicValue> values) {
}
