package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.domain.core.model.PackageLine;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.domain.core.rule.LaborPricing;
import com.bradox.erp.repair.domain.core.valueobject.LineStatus;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.domain.core.valueobject.PricingMode;
import com.bradox.erp.repair.domain.core.valueobject.SubletStatus;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.ports.output.ProductLookupPort;
import com.bradox.erp.repair.service.domain.ports.output.repository.LaborRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.LineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builds and saves repair lines from a command, a labor-guide entry or a package; shared by lines and inspection. */
@Component
class LineFactory {

    private final LineRepository lines;
    private final LaborRepository labor;
    private final ProductLookupPort products;
    private final RepairAccess access;

    LineFactory(LineRepository lines, LaborRepository labor, ProductLookupPort products, RepairAccess access) {
        this.lines = lines;
        this.labor = labor;
        this.products = products;
        this.access = access;
    }

    RepairLine create(CompanyId companyId, UUID orderId, AddLineCommand c, Settings settings, UUID findingId, UUID packageId,
                      String defaultSection) {
        LineType type = c.type();
        String description = c.description();
        BigDecimal unitPrice = c.unitPrice();
        BigDecimal qty = c.qty() == null ? BigDecimal.ONE : c.qty();
        PricingMode mode = null;
        Integer minutes = null;
        BigDecimal rate = null;
        UUID guideId = null;

        if (c.productId() != null && (type == LineType.PART || type == LineType.CUSTOMER_PART)) {
            ProductLookupPort.Product p = products.find(companyId, c.productId())
                    .orElseThrow(() -> new RepairDomainException("error.repair.product.notFound", null, "Product not found"));
            if (description == null || description.isBlank()) {
                description = p.name();
            }
            if (unitPrice == null && type == LineType.PART) {
                unitPrice = p.listPrice();
            }
        }
        if (type == LineType.OPERATION) {
            mode = c.pricingMode() == null ? PricingMode.FLAT_RATE : c.pricingMode();
            minutes = c.standardMinutes();
            if (c.laborGuideId() != null) {
                GuideEntry g = labor.findGuide(companyId, c.laborGuideId())
                        .orElseThrow(() -> new RepairDomainException("error.repair.guide.notFound", null, "Labor guide entry not found"));
                guideId = g.id();
                if (minutes == null) {
                    minutes = g.standardMinutes();
                }
                if (description == null || description.isBlank()) {
                    description = firstNonBlank(g.descriptionEn(), g.descriptionAr(), g.descriptionKu(), g.code());
                }
                LaborCategory cat = g.laborCategoryId() == null ? null : labor.findCategory(companyId, g.laborCategoryId()).orElse(null);
                rate = LaborPricing.resolveRate(cat == null ? null : cat.hourlyRate(), settings.defaultHourlyRate());
            } else {
                rate = LaborPricing.resolveRate(null, settings.defaultHourlyRate());
            }
            if (unitPrice == null && mode == PricingMode.FLAT_RATE && minutes != null) {
                unitPrice = LaborPricing.flatRatePrice(minutes, rate);
            }
        }
        if (type == LineType.CUSTOMER_PART || type == LineType.REMOVED_PART) {
            unitPrice = BigDecimal.ZERO;
        }
        BigDecimal discount = type == LineType.CUSTOMER_PART || type == LineType.REMOVED_PART || c.discountPercent() == null
                ? BigDecimal.ZERO : c.discountPercent();
        boolean needsApproval = discount.compareTo(settings.advisorDiscountLimitPercent()) > 0
                && !access.can(RepairPermissions.DISCOUNT_APPROVE);

        RepairLine line = new RepairLine(UUID.randomUUID(), companyId.getId(), orderId, type,
                c.sectionLabel() != null ? c.sectionLabel() : defaultSection, nextSequence(companyId, orderId), c.productId(),
                description, qty, unitPrice == null ? BigDecimal.ZERO : unitPrice, discount, LineStatus.DRAFT, needsApproval,
                findingId, packageId, guideId, mode, minutes, rate, c.assigneeEmployeeId(),
                type == LineType.SUBLET ? c.vendorPartnerId() : null, type == LineType.SUBLET ? c.vendorCost() : null,
                type == LineType.SUBLET ? SubletStatus.REQUESTED : null, null, access.now(), access.actorLabel());
        return lines.save(line);
    }

    List<RepairLine> fromPackage(CompanyId companyId, UUID orderId, ServicePackage pkg, Settings settings, UUID findingId) {
        List<RepairLine> out = new ArrayList<>();
        for (PackageLine pl : pkg.lines()) {
            AddLineCommand cmd = new AddLineCommand(pl.type(), pkg.name(), pl.productId(), pl.description(), pl.qty(), null, null,
                    pl.laborGuideId(), pl.type() == LineType.OPERATION ? PricingMode.FLAT_RATE : null, pl.standardMinutes(), null, null,
                    null);
            out.add(create(companyId, orderId, cmd, settings, findingId, pkg.id(), pkg.name()));
        }
        return out;
    }

    private int nextSequence(CompanyId companyId, UUID orderId) {
        return lines.listByOrder(companyId, orderId).stream().mapToInt(RepairLine::sequence).max().orElse(0) + 1;
    }

    LineResponse toResponse(RepairLine l, boolean prices) {
        boolean billable = l.type() == LineType.PART || l.type() == LineType.OPERATION || l.type() == LineType.SUBLET;
        return new LineResponse(l.id(), l.orderId(), l.type().name(), l.sectionLabel(), l.sequence(), l.productId(), l.description(),
                l.qty(), prices ? l.unitPrice() : null, prices ? l.discountPercent() : null,
                prices ? LaborPricing.lineTotal(l.qty(), l.unitPrice(), l.discountPercent()) : null, l.status().name(),
                l.needsDiscountApproval(), billable && l.unitPrice().signum() == 0, l.fromFindingId(), l.fromPackageId(),
                l.laborGuideId(), l.pricingMode() == null ? null : l.pricingMode().name(), l.standardMinutes(),
                prices ? l.laborRate() : null, l.assigneeEmployeeId(), l.vendorPartnerId(), prices ? l.vendorCost() : null,
                l.subletStatus() == null ? null : l.subletStatus().name(), l.disposition() == null ? null : l.disposition().name(),
                l.createdAt(), l.createdBy());
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
