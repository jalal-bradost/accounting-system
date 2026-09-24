package com.bradox.delin.sales.application.rest;

import com.bradox.delin.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.service.domain.dto.StockPickingResponse;
import com.bradox.delin.inventory.service.domain.dto.ValidatePickingCommand;
import com.bradox.delin.platform.application.dto.PageResponse;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import com.bradox.delin.sales.domain.core.SalesOrderState;
import com.bradox.delin.sales.service.domain.SalesDashboardService;
import com.bradox.delin.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.bradox.delin.sales.service.domain.dto.CreateSalesOrderCommand;
import com.bradox.delin.sales.service.domain.dto.CreateSalesReturnCommand;
import com.bradox.delin.sales.service.domain.dto.SalesDashboardResponse;
import com.bradox.delin.sales.service.domain.dto.SalesOrderResponse;
import com.bradox.delin.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.bradox.delin.sales.service.domain.ports.input.SalesApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/sales", produces = "application/json")
public class SalesController {

    private final SalesApplicationService salesApplicationService;
    private final SalesDashboardService salesDashboardService;

    public SalesController(SalesApplicationService salesApplicationService,
                           SalesDashboardService salesDashboardService) {
        this.salesApplicationService = salesApplicationService;
        this.salesDashboardService = salesDashboardService;
    }

    @GetMapping("/dashboard")
    @RequiresPermission("sales.order.read")
    public ResponseEntity<SalesDashboardResponse> dashboard(
            @CurrentCompany CompanyId companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesDashboardService.getDashboard(companyId.getId(), from, to));
    }

    @PostMapping("/orders")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> createOrder(@CurrentCompany CompanyId companyId,
                                                          @Valid @RequestBody CreateSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.createSalesOrder(cmd));
    }

    @PutMapping("/orders/{id}")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> updateOrder(@CurrentCompany CompanyId companyId,
                                                          @PathVariable UUID id,
                                                          @Valid @RequestBody CreateSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.updateSalesOrder(id, cmd));
    }

    @GetMapping("/orders")
    @RequiresPermission("sales.order.read")
    public ResponseEntity<PageResponse<SalesOrderSummaryResponse>> listOrders(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) UUID customerPartnerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        SalesOrderState stateFilter = null;
        if (state != null && !state.isBlank()) {
            stateFilter = SalesOrderState.valueOf(state.trim().toUpperCase(Locale.ROOT));
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = salesApplicationService.searchSalesOrders(
                companyId.getId(), stateFilter, customerPartnerId, q, pageable);
        return ResponseEntity.ok(PageResponse.of(result, Function.identity()));
    }

    @GetMapping("/orders/{id}")
    @RequiresPermission("sales.order.read")
    public ResponseEntity<SalesOrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.getSalesOrder(id));
    }

    @PostMapping("/orders/{id}/send")
    @RequiresPermission("sales.order.confirm")
    public ResponseEntity<SalesOrderResponse> send(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.sendQuotation(id));
    }

    @PostMapping("/orders/{id}/confirm")
    @RequiresPermission("sales.order.confirm")
    public ResponseEntity<SalesOrderResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.confirmSalesOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.cancelSalesOrder(id));
    }

    @PostMapping("/orders/{id}/lock")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.lockSalesOrder(id));
    }

    @PostMapping("/orders/{id}/unlock")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> unlock(@PathVariable UUID id) {
        return ResponseEntity.ok(salesApplicationService.unlockSalesOrder(id));
    }

    @PostMapping("/orders/{id}/return")
    @RequiresPermission("sales.order.confirm")
    public ResponseEntity<StockPickingResponse> createReturn(@PathVariable UUID id,
                                                             @RequestBody(required = false) CreateSalesReturnCommand body) {
        return ResponseEntity.ok(salesApplicationService.createReturnFromSalesOrder(
                id, body != null ? body : new CreateSalesReturnCommand()));
    }

    @PostMapping("/deliveries/{pickingId}/validate")
    @RequiresPermission("sales.order.confirm")
    public ResponseEntity<StockPickingResponse> validateDelivery(@PathVariable UUID pickingId,
                                                                 @RequestBody(required = false) ValidatePickingCommand body) {
        return ResponseEntity.ok(salesApplicationService.validateDeliveryPicking(pickingId, body));
    }

    @PostMapping("/customer-invoices/from-order")
    @RequiresPermission("sales.invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> createInvoiceFromOrder(@CurrentCompany CompanyId companyId,
                                                                          @Valid @RequestBody CreateCustomerInvoiceFromSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.createCustomerInvoiceFromSalesOrder(cmd));
    }

    @PostMapping("/customer-credit-notes/from-order")
    @RequiresPermission("sales.invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> createCreditNoteFromOrder(@CurrentCompany CompanyId companyId,
                                                                             @Valid @RequestBody CreateCustomerInvoiceFromSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.createCustomerCreditNoteFromSalesOrder(cmd));
    }
}
