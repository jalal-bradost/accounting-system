package com.bradox.delin.pos.service.domain.ports.input;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.pos.service.domain.dto.CheckoutPosOrderCommand;
import com.bradox.delin.pos.service.domain.dto.ClosePosSessionCommand;
import com.bradox.delin.pos.service.domain.dto.CreatePosOrderCommand;
import com.bradox.delin.pos.service.domain.dto.OpenPosSessionCommand;
import com.bradox.delin.pos.service.domain.dto.PosCatalogItemResponse;
import com.bradox.delin.pos.service.domain.dto.PosConfigCardResponse;
import com.bradox.delin.pos.service.domain.dto.PosConfigCommand;
import com.bradox.delin.pos.service.domain.dto.PosConfigResponse;
import com.bradox.delin.pos.service.domain.dto.PosOrderLineCommand;
import com.bradox.delin.pos.service.domain.dto.PosOrderResponse;
import com.bradox.delin.pos.service.domain.dto.PosReceiptResponse;
import com.bradox.delin.pos.service.domain.dto.PosSessionResponse;
import com.bradox.delin.pos.service.domain.dto.RegisterPosPaymentCommand;
import com.bradox.delin.pos.service.domain.dto.UpdatePosOrderLineCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PosApplicationService {
    PosConfigResponse createConfig(@Valid PosConfigCommand command);

    List<PosConfigResponse> listConfigs(CompanyId companyId);

    List<PosConfigCardResponse> listConfigCards(CompanyId companyId);

    PosSessionResponse getSession(UUID sessionId);

    PosSessionResponse getOpenSessionForConfig(CompanyId companyId, UUID configId);

    PosSessionResponse openSession(@Valid OpenPosSessionCommand command);

    PosSessionResponse closeSession(UUID sessionId, @Valid ClosePosSessionCommand command);

    Page<PosCatalogItemResponse> searchCatalog(CompanyId companyId, UUID sessionId, String query, UUID categoryId,
                                                 Pageable pageable);

    /** Exact barcode or SKU catalog lookup for the open session warehouse. */
    PosCatalogItemResponse findCatalogByBarcode(CompanyId companyId, UUID sessionId, String barcode);

    PosOrderResponse createOrder(@Valid CreatePosOrderCommand command);

    PosOrderResponse addOrderLine(UUID orderId, @Valid PosOrderLineCommand command);

    PosOrderResponse updateOrderLine(UUID orderId, UUID lineId, @Valid UpdatePosOrderLineCommand command);

    PosOrderResponse registerPayment(UUID orderId, @Valid RegisterPosPaymentCommand command);

    PosOrderResponse finalizeOrder(UUID orderId);

    PosOrderResponse checkout(@Valid CheckoutPosOrderCommand command);

    PosOrderResponse getOrder(UUID orderId);

    /** Completed (finalized) POS orders for the register session order list. */
    List<PosOrderResponse> listSessionOrders(UUID sessionId);

    PosReceiptResponse getReceipt(UUID receiptId);
}
