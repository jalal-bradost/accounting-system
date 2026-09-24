package com.bradox.delin.inventory.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.domain.core.entity.StockPicking;
import com.bradox.delin.inventory.domain.core.valueobject.PickingState;
import com.bradox.delin.inventory.domain.core.valueobject.PickingType;
import com.bradox.delin.inventory.domain.core.valueobject.StockPickingId;
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
