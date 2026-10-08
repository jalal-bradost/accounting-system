package com.bradox.erp.repair.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.repair.domain.core.model.Finding;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.AddPackageCommand;
import com.bradox.erp.repair.service.domain.dto.DispositionCommand;
import com.bradox.erp.repair.service.domain.dto.FindingCommand;
import com.bradox.erp.repair.service.domain.dto.InspectionResponse;
import com.bradox.erp.repair.service.domain.dto.LineFromFindingCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.LinesResponse;
import com.bradox.erp.repair.service.domain.dto.OrderActivity;
import com.bradox.erp.repair.service.domain.dto.SaveInspectionCommand;
import com.bradox.erp.repair.service.domain.dto.TemplateCommand;
import com.bradox.erp.repair.service.domain.dto.UpdateLineCommand;
import com.bradox.erp.repair.service.domain.ports.input.InspectionApplicationService;
import com.bradox.erp.repair.service.domain.ports.input.LineApplicationService;
import com.bradox.erp.repair.service.domain.ports.input.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Lines, inspection and findings of one repair order (REP-01 to REP-03). {@code orderId} is the maintenance order id. */
@RestController
@RequestMapping(value = "/api/v1/repair", produces = "application/json")
public class OrderRepairController {

    private final LineApplicationService lines;
    private final InspectionApplicationService inspections;
    private final OrderApplicationService orders;

    public OrderRepairController(LineApplicationService lines, InspectionApplicationService inspections,
                                 OrderApplicationService orders) {
        this.orders = orders;
        this.lines = lines;
        this.inspections = inspections;
    }

    // ---- orders

    @GetMapping("/orders")
    public List<OrderActivity> orders(@CurrentCompany CompanyId companyId) {
        return orders.recent(companyId);
    }

    // ---- lines

    @GetMapping("/orders/{orderId}/lines")
    public LinesResponse lines(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId) {
        return lines.list(companyId, orderId);
    }

    @PostMapping("/orders/{orderId}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    public LineResponse addLine(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId, @Valid @RequestBody AddLineCommand c) {
        return lines.add(companyId, orderId, c);
    }

    @PostMapping("/orders/{orderId}/lines/from-package")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LineResponse> addPackage(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId,
                                         @Valid @RequestBody AddPackageCommand c) {
        return lines.addPackage(companyId, orderId, c);
    }

    @PutMapping("/lines/{lineId}")
    public LineResponse updateLine(@CurrentCompany CompanyId companyId, @PathVariable UUID lineId, @RequestBody UpdateLineCommand c) {
        return lines.update(companyId, lineId, c);
    }

    @DeleteMapping("/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLine(@CurrentCompany CompanyId companyId, @PathVariable UUID lineId) {
        lines.delete(companyId, lineId);
    }

    @PostMapping("/lines/{lineId}/approve-discount")
    public LineResponse approveDiscount(@CurrentCompany CompanyId companyId, @PathVariable UUID lineId) {
        return lines.approveDiscount(companyId, lineId);
    }

    @PutMapping("/lines/{lineId}/disposition")
    public LineResponse disposition(@CurrentCompany CompanyId companyId, @PathVariable UUID lineId,
                                    @Valid @RequestBody DispositionCommand c) {
        return lines.setDisposition(companyId, lineId, c);
    }

    // ---- inspection templates

    @GetMapping("/inspection-templates")
    public List<InspectionTemplate> templates(@CurrentCompany CompanyId companyId,
                                              @RequestParam(defaultValue = "false") boolean includeInactive) {
        return inspections.templates(companyId, includeInactive);
    }

    @PostMapping("/inspection-templates")
    public InspectionTemplate createTemplate(@CurrentCompany CompanyId companyId, @Valid @RequestBody TemplateCommand c) {
        return inspections.saveTemplate(companyId, null, c);
    }

    @PutMapping("/inspection-templates/{id}")
    public InspectionTemplate updateTemplate(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                             @Valid @RequestBody TemplateCommand c) {
        return inspections.saveTemplate(companyId, id, c);
    }

    // ---- inspection and findings

    @GetMapping("/orders/{orderId}/inspection")
    public InspectionResponse inspection(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId) {
        return inspections.get(companyId, orderId);
    }

    @PostMapping("/orders/{orderId}/inspection")
    public InspectionResponse startInspection(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId,
                                              @RequestParam(required = false) UUID templateId) {
        return inspections.start(companyId, orderId, templateId);
    }

    @PutMapping("/orders/{orderId}/inspection")
    public InspectionResponse saveInspection(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId,
                                             @RequestBody SaveInspectionCommand c) {
        return inspections.save(companyId, orderId, c);
    }

    @PostMapping("/orders/{orderId}/inspection/sign-off")
    public InspectionResponse signOff(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId) {
        return inspections.signOff(companyId, orderId);
    }

    @GetMapping("/orders/{orderId}/findings")
    public List<Finding> findings(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId) {
        return inspections.findings(companyId, orderId);
    }

    @PostMapping("/orders/{orderId}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public Finding addFinding(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId, @Valid @RequestBody FindingCommand c) {
        return inspections.addFinding(companyId, orderId, c);
    }

    @PostMapping("/orders/{orderId}/findings/{findingId}/add-to-quote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LineResponse> addToQuote(@CurrentCompany CompanyId companyId, @PathVariable UUID orderId,
                                         @PathVariable UUID findingId, @RequestBody LineFromFindingCommand c) {
        return inspections.addToQuote(companyId, orderId, findingId, c);
    }
}
