package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.dto.OrderActivity;

import java.util.List;

public interface OrderActivityRepository {

    /** Orders with lines, findings or an inspection, most recent activity first. */
    List<OrderActivity> recent(CompanyId companyId, int limit);
}
