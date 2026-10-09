package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.dto.RepairOrderCommand;
import com.bradox.erp.repair.service.domain.dto.RepairOrderResponse;

import java.util.List;
import java.util.UUID;

public interface RepairOrderApplicationService {

    List<RepairOrderResponse> list(CompanyId companyId);

    RepairOrderResponse get(CompanyId companyId, UUID id);

    RepairOrderResponse create(CompanyId companyId, RepairOrderCommand command);

    RepairOrderResponse update(CompanyId companyId, UUID id, RepairOrderCommand command);

    /** Confirms the order and, unless it is under warranty, creates the sale quotation for its parts. */
    RepairOrderResponse confirm(CompanyId companyId, UUID id);

    RepairOrderResponse done(CompanyId companyId, UUID id);

    RepairOrderResponse cancel(CompanyId companyId, UUID id);
}
