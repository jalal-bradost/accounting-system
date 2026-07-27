package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BulkCreateAttendanceCommand {

    private UUID companyId;
    @NotEmpty
    @Valid
    private List<CreateAttendanceCommand> items;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public List<CreateAttendanceCommand> getItems() { return items; }
    public void setItems(List<CreateAttendanceCommand> v) { this.items = v; }
}
