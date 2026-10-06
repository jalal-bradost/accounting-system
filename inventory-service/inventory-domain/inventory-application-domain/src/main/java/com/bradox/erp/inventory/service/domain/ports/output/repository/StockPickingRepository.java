package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.StockPicking;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.domain.core.valueobject.StockPickingId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StockPickingRepository {
    StockPicking save(StockPicking picking);
    Optional<StockPicking> findById(StockPickingId id);

    /** Pickings created from the given one: backorders (same type) and returns (opposite type). */
    List<StockPicking> findByBackorderOf(StockPickingId id);
    Page<StockPicking> search(CompanyId companyId,
                              PickingType pickingType,
                              PickingState state,
                              Pageable pageable);
}
