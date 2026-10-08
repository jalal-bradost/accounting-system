package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.StartTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.StopTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.TimerResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.TimerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/timesheet/timer", produces = "application/json")
public class TimerController {

    private final TimerApplicationService timer;

    public TimerController(TimerApplicationService timer) {
        this.timer = timer;
    }

    /** 200 with the running timer, or 204 when there is none. */
    @GetMapping
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<TimerResponse> current(@CurrentCompany CompanyId companyId) {
        TimerResponse t = timer.current(companyId);
        return t == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(t);
    }

    @PostMapping("/start")
    @RequiresPermission("tsh.entry.own")
    public TimerResponse start(@CurrentCompany CompanyId companyId, @Valid @RequestBody StartTimerCommand command) {
        return timer.start(companyId, command);
    }

    @PostMapping("/stop")
    @RequiresPermission("tsh.entry.own")
    public List<EntryResponse> stop(@CurrentCompany CompanyId companyId, @RequestBody(required = false) StopTimerCommand command) {
        return timer.stop(companyId, command);
    }

    @DeleteMapping
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<Void> discard(@CurrentCompany CompanyId companyId) {
        timer.discard(companyId);
        return ResponseEntity.noContent().build();
    }
}
