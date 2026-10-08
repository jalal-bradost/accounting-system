package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.dto.OrderActivity;

import java.util.List;

public interface OrderApplicationService {

    List<OrderActivity> recent(CompanyId companyId);
}
