package com.bradox.erp.inventory.service.domain.ports.input;

import com.bradox.erp.inventory.service.domain.dto.OpeningStockCommand;
import com.bradox.erp.inventory.service.domain.dto.OpeningStockReport;
import jakarta.validation.Valid;

public interface OpeningStockApplicationService {

    OpeningStockReport importOpeningStock(@Valid OpeningStockCommand command);
}
