package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateRequestCommand(@NotBlank String name, String signingOrder, String message,
                                   Integer reminderEveryDays, List<SignerInput> signers, List<FieldDto> fields) {
}
