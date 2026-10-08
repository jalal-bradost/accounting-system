package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.LineType;

import java.util.UUID;

/** "Add to quote" on a finding or an inspection item: a draft line prefilled from it (REP-02 #3). */
public record LineFromFindingCommand(LineType type, String description, UUID productId, UUID laborGuideId, UUID packageId) {
}
