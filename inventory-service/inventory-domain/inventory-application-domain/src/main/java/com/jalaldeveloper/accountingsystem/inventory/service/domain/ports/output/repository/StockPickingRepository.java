package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockPicking;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StockPickingRepository {
    StockPicking save(StockPicking picking);
    Optional<StockPicking> findById(StockPickingId id);
    Page<StockPicking> search(CompanyId companyId,
                              PickingType pickingType,
                              PickingState state,
                              Pageable pageable);
}
