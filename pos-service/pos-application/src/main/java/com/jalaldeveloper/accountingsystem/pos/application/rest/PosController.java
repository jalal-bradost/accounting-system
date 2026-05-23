package com.jalaldeveloper.accountingsystem.pos.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
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
import com.jalaldeveloper.accountingsystem.pos.service.domain.ports.input.PosApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/pos", produces = "application/json")
public class PosController {
    private final PosApplicationService posApplicationService;

    public PosController(PosApplicationService posApplicationService) {
        this.posApplicationService = posApplicationService;
    }

    @GetMapping("/configs")
    @RequiresPermission("pos.config.read")
    public ResponseEntity<List<PosConfigResponse>> listConfigs(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(posApplicationService.listConfigs(companyId));
    }

    @PostMapping("/configs")
    @RequiresPermission("pos.config.write")
    public ResponseEntity<PosConfigResponse> createConfig(@CurrentCompany CompanyId companyId,
                                                          @Valid @RequestBody PosConfigCommand command) {
        command.setCompanyId(companyId.getId());
        return ResponseEntity.ok(posApplicationService.createConfig(command));
    }

    @PostMapping("/sessions")
    @RequiresPermission("pos.session.open")
    public ResponseEntity<PosSessionResponse> openSession(@CurrentCompany CompanyId companyId,
                                                          @Valid @RequestBody OpenPosSessionCommand command) {
        command.setCompanyId(companyId.getId());
        return ResponseEntity.ok(posApplicationService.openSession(command));
    }

    @PostMapping("/sessions/{sessionId}/close")
    @RequiresPermission("pos.session.close")
    public ResponseEntity<PosSessionResponse> closeSession(@PathVariable UUID sessionId,
                                                           @Valid @RequestBody ClosePosSessionCommand command) {
        return ResponseEntity.ok(posApplicationService.closeSession(sessionId, command));
    }

    @GetMapping("/catalog")
    @RequiresPermission("pos.order.read")
    public ResponseEntity<PageResponse<PosCatalogItemResponse>> catalog(@CurrentCompany CompanyId companyId,
                                                                        @RequestParam UUID sessionId,
                                                                        @RequestParam(required = false) String search,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        var result = posApplicationService.searchCatalog(companyId, sessionId, search, pageable);
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @PostMapping("/orders")
    @RequiresPermission("pos.order.write")
    public ResponseEntity<PosOrderResponse> createOrder(@CurrentCompany CompanyId companyId,
                                                        @Valid @RequestBody CreatePosOrderCommand command) {
        command.setCompanyId(companyId.getId());
        return ResponseEntity.ok(posApplicationService.createOrder(command));
    }

    @PostMapping("/orders/{orderId}/lines")
    @RequiresPermission("pos.order.write")
    public ResponseEntity<PosOrderResponse> addLine(@PathVariable UUID orderId,
                                                    @Valid @RequestBody PosOrderLineCommand command) {
        return ResponseEntity.ok(posApplicationService.addOrderLine(orderId, command));
    }

    @PatchMapping("/orders/{orderId}/lines/{lineId}")
    @RequiresPermission("pos.order.write")
    public ResponseEntity<PosOrderResponse> updateLine(@PathVariable UUID orderId,
                                                       @PathVariable UUID lineId,
                                                       @Valid @RequestBody UpdatePosOrderLineCommand command) {
        return ResponseEntity.ok(posApplicationService.updateOrderLine(orderId, lineId, command));
    }

    @PostMapping("/orders/{orderId}/payments")
    @RequiresPermission("pos.order.pay")
    public ResponseEntity<PosOrderResponse> registerPayment(@PathVariable UUID orderId,
                                                            @Valid @RequestBody RegisterPosPaymentCommand command) {
        return ResponseEntity.ok(posApplicationService.registerPayment(orderId, command));
    }

    @PostMapping("/orders/{orderId}/finalize")
    @RequiresPermission("pos.order.finalize")
    public ResponseEntity<PosOrderResponse> finalizeOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(posApplicationService.finalizeOrder(orderId));
    }

    @PostMapping("/checkout")
    @RequiresPermission("pos.order.finalize")
    public ResponseEntity<PosOrderResponse> checkout(@CurrentCompany CompanyId companyId,
                                                     @Valid @RequestBody CheckoutPosOrderCommand command) {
        command.setCompanyId(companyId.getId());
        return ResponseEntity.ok(posApplicationService.checkout(command));
    }

    @GetMapping("/orders/{orderId}")
    @RequiresPermission("pos.order.read")
    public ResponseEntity<PosOrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(posApplicationService.getOrder(orderId));
    }

    @GetMapping("/receipts/{receiptId}")
    @RequiresPermission("pos.receipt.read")
    public ResponseEntity<PosReceiptResponse> getReceipt(@PathVariable UUID receiptId) {
        return ResponseEntity.ok(posApplicationService.getReceipt(receiptId));
    }
}
