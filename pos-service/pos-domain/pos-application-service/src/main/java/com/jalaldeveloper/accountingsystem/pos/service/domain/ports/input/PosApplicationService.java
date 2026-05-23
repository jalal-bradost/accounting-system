package com.jalaldeveloper.accountingsystem.pos.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.CheckoutPosOrderCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.ClosePosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.CreatePosOrderCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.OpenPosSessionCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosCatalogItemResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosConfigResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderLineCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosOrderResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosReceiptResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.PosSessionResponse;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.RegisterPosPaymentCommand;
import com.jalaldeveloper.accountingsystem.pos.service.domain.dto.UpdatePosOrderLineCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PosApplicationService {
    PosConfigResponse createConfig(@Valid PosConfigCommand command);

    List<PosConfigResponse> listConfigs(CompanyId companyId);

    PosSessionResponse openSession(@Valid OpenPosSessionCommand command);

    PosSessionResponse closeSession(UUID sessionId, @Valid ClosePosSessionCommand command);

    Page<PosCatalogItemResponse> searchCatalog(CompanyId companyId, UUID sessionId, String query, Pageable pageable);

    PosOrderResponse createOrder(@Valid CreatePosOrderCommand command);

    PosOrderResponse addOrderLine(UUID orderId, @Valid PosOrderLineCommand command);

    PosOrderResponse updateOrderLine(UUID orderId, UUID lineId, @Valid UpdatePosOrderLineCommand command);

    PosOrderResponse registerPayment(UUID orderId, @Valid RegisterPosPaymentCommand command);

    PosOrderResponse finalizeOrder(UUID orderId);

    PosOrderResponse checkout(@Valid CheckoutPosOrderCommand command);

    PosOrderResponse getOrder(UUID orderId);

    PosReceiptResponse getReceipt(UUID receiptId);
}
