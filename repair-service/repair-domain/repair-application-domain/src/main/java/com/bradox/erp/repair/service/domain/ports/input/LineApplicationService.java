package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.AddPackageCommand;
import com.bradox.erp.repair.service.domain.dto.DispositionCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.LinesResponse;
import com.bradox.erp.repair.service.domain.dto.UpdateLineCommand;

import java.util.List;
import java.util.UUID;

public interface LineApplicationService {

    LinesResponse list(CompanyId companyId, UUID orderId);

    LineResponse add(CompanyId companyId, UUID orderId, AddLineCommand command);

    List<LineResponse> addPackage(CompanyId companyId, UUID orderId, AddPackageCommand command);

    LineResponse update(CompanyId companyId, UUID lineId, UpdateLineCommand command);

    void delete(CompanyId companyId, UUID lineId);

    /** A Manager clears a line held for a discount above the advisor's limit (BR-REP-09). */
    LineResponse approveDiscount(CompanyId companyId, UUID lineId);

    LineResponse setDisposition(CompanyId companyId, UUID lineId, DispositionCommand command);
}
