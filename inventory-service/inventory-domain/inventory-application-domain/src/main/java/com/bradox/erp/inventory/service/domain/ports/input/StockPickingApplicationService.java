package com.bradox.erp.inventory.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.PickingState;
import com.bradox.erp.inventory.domain.core.valueobject.PickingType;
import com.bradox.erp.inventory.service.domain.dto.CreateStockPickingCommand;
import com.bradox.erp.inventory.service.domain.dto.InventoryAdjustmentCommand;
import com.bradox.erp.inventory.service.domain.dto.ReturnPickingCommand;
import com.bradox.erp.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.erp.inventory.service.domain.dto.ValidatePickingCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockPickingApplicationService {

    StockPickingResponse createPicking(@Valid CreateStockPickingCommand command);

    StockPickingResponse confirmPicking(UUID pickingId);

    /** Reserve stock for the picking; lifts state to ASSIGNED (or CONFIRMED on partial). */
    StockPickingResponse assignPicking(UUID pickingId);

    /**
     * Validate the picking. Mutates quants, appends valuation layers, posts the journal
     * entry, and (when {@code createBackorder} is true) splits unfulfilled qty into a new
     * draft picking.
     */
    StockPickingResponse validatePicking(UUID pickingId, ValidatePickingCommand command);

    StockPickingResponse cancelPicking(UUID pickingId);

    /**
     * Reverse a DONE picking by creating a return picking (opposite directions, same product
     * lines). The returned picking is created in DRAFT state and must be confirmed/validated
     * by the caller (so the user can adjust qty before re-posting accounting).
     */
    StockPickingResponse returnPicking(UUID pickingId);

    /**
     * Same as {@link #returnPicking(UUID)} with optional per-move return quantities
     * (original move id → qty). Omitted moves use full picked qty; qty ≤ 0 skips the move.
     */
    StockPickingResponse returnPicking(UUID pickingId, ReturnPickingCommand command);

    /** Apply a manual stock adjustment for a single (product, location). */
    StockPickingResponse adjustInventory(@Valid InventoryAdjustmentCommand command);

    StockPickingResponse getPicking(UUID pickingId);

    Page<StockPickingResponse> searchPickings(CompanyId companyId,
                                              PickingType pickingType,
                                              PickingState state,
                                              Pageable pageable);
}
