package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.rule.LaborGuideCsv;

import java.util.List;

public record GuideImportResponse(int created, int updated, int rejected, boolean applied, List<LaborGuideCsv.RowError> errors) {
}
