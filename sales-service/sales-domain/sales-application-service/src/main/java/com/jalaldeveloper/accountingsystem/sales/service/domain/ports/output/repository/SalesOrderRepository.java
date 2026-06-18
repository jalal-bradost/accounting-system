package com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository {

    SalesOrder save(SalesOrder order);

    Optional<SalesOrder> findByIdWithLines(UUID id);

    Optional<SalesOrder> findByCompanyIdAndName(UUID companyId, String name);

    Page<SalesOrder> search(UUID companyId,
                            SalesOrderState state,
                            UUID customerPartnerId,
                            String q,
                            Pageable pageable);

    void flush();
}
