package com.bradox.delin.expense.application.rest;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.entity.ExpenseState;
import com.bradox.delin.expense.service.domain.dto.ExpenseResponse;
import com.bradox.delin.expense.service.domain.dto.ExpenseSummaryResponse;
import com.bradox.delin.expense.service.domain.dto.RegisterExpensePaymentCommand;
import com.bradox.delin.expense.service.domain.dto.SaveExpenseCommand;
import com.bradox.delin.expense.service.domain.ports.input.ExpenseApplicationService;
import com.bradox.delin.platform.security.RequiresPermission;
import com.bradox.delin.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/expenses", produces = "application/json")
public class ExpenseController {

    private final ExpenseApplicationService service;

    public ExpenseController(ExpenseApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("expense.read")
    public ResponseEntity<List<ExpenseResponse>> list(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) ExpenseState state) {
        return ResponseEntity.ok(service.list(companyId, employeeId, state));
    }

    @GetMapping("/summary")
    @RequiresPermission("expense.read")
    public ResponseEntity<ExpenseSummaryResponse> summary(
            @CurrentCompany CompanyId companyId,
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(service.summary(companyId, employeeId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("expense.read")
    public ResponseEntity<ExpenseResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody SaveExpenseCommand command) {
        return ResponseEntity.ok(service.create(command));
    }

    @PutMapping("/{id}")
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveExpenseCommand command) {
        return ResponseEntity.ok(service.update(id, command));
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/approve")
    @RequiresPermission("expense.approve")
    public ResponseEntity<ExpenseResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/refuse")
    @RequiresPermission("expense.approve")
    public ResponseEntity<ExpenseResponse> refuse(@PathVariable UUID id) {
        return ResponseEntity.ok(service.refuse(id));
    }

    @PostMapping("/{id}/post")
    @RequiresPermission("expense.post")
    public ResponseEntity<ExpenseResponse> post(@PathVariable UUID id) {
        return ResponseEntity.ok(service.post(id));
    }

    @PostMapping("/{id}/pay")
    @RequiresPermission("expense.post")
    public ResponseEntity<ExpenseResponse> pay(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterExpensePaymentCommand command) {
        return ResponseEntity.ok(service.registerPayment(id, command));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}
