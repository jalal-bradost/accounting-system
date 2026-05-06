package com.jalaldeveloper.accountingsystem.sales.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice.CustomerInvoiceResponse;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.application.dto.PageResponse;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateCustomerInvoiceFromSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.CreateSalesOrderCommand;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.dto.SalesOrderSummaryResponse;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.input.SalesApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api/v1/sales", produces = "application/json")
public class SalesController {

    private final SalesApplicationService salesApplicationService;

    public SalesController(SalesApplicationService salesApplicationService) {
        this.salesApplicationService = salesApplicationService;
    }

    @PostMapping("/orders")
    @RequiresPermission("sales.order.write")
    public ResponseEntity<SalesOrderResponse> createOrder(@CurrentCompany CompanyId companyId,
                                                          @Valid @RequestBody CreateSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.createSalesOrder(cmd));
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

    @PostMapping("/customer-invoices/from-order")
    @RequiresPermission("sales.invoice.write")
    public ResponseEntity<CustomerInvoiceResponse> createInvoiceFromOrder(@CurrentCompany CompanyId companyId,
                                                                          @Valid @RequestBody CreateCustomerInvoiceFromSalesOrderCommand cmd) {
        cmd.setCompanyId(companyId.getId());
        return ResponseEntity.ok(salesApplicationService.createCustomerInvoiceFromSalesOrder(cmd));
    }
}
