package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateTemplateCommand(@NotBlank String name, String category, String defaultMessage, Integer defaultValidityDays,
                                    boolean active, List<RoleDto> roles, List<FieldDto> fields) {
}
