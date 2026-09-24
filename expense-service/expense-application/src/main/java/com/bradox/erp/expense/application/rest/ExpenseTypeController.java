package com.bradox.erp.expense.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.expense.service.domain.dto.ExpenseTypeResponse;
import com.bradox.erp.expense.service.domain.dto.SaveExpenseTypeCommand;
import com.bradox.erp.expense.service.domain.ports.input.ExpenseTypeApplicationService;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/expense-types", produces = "application/json")
public class ExpenseTypeController {

    private final ExpenseTypeApplicationService service;

    public ExpenseTypeController(ExpenseTypeApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("expense.read")
    public ResponseEntity<List<ExpenseTypeResponse>> list(@CurrentCompany CompanyId companyId) {
        return ResponseEntity.ok(service.list(companyId));
    }

    @PostMapping
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseTypeResponse> create(@Valid @RequestBody SaveExpenseTypeCommand command) {
        return ResponseEntity.ok(service.create(command));
    }

    @PutMapping("/{id}")
    @RequiresPermission("expense.write")
    public ResponseEntity<ExpenseTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveExpenseTypeCommand command) {
        return ResponseEntity.ok(service.update(id, command));
    }
}
