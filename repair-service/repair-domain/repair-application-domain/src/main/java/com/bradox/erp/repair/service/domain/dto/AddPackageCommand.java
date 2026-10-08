package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddPackageCommand(@NotNull UUID packageId) {
}
