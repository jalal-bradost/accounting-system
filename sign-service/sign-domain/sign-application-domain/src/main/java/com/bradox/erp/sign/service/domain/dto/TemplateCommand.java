package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateCommand(@NotBlank @Size(max = 255) String name, @Valid List<FieldDto> fields) {
}
