package com.bradox.erp.sales.service.domain;

import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.service.domain.ports.input.UomApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.sales.domain.core.SalInvoicePolicy;
import com.bradox.erp.sales.domain.core.SalesDomainException;
import com.bradox.erp.sales.domain.core.SalesOrderState;
import com.bradox.erp.sales.domain.core.entity.SalesOrder;
import com.bradox.erp.sales.domain.core.entity.SalesOrderLine;
import com.bradox.erp.sales.service.domain.dto.SalesTimesheetLineResponse;
import com.bradox.erp.sales.service.domain.ports.input.SalesTimesheetBillingService;
import com.bradox.erp.sales.service.domain.ports.output.repository.SalesOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
class SalesTimesheetBillingServiceImpl implements SalesTimesheetBillingService {

    private static final int ORDER_PAGE = 200;

    private final SalesOrderRepository orders;
    private final ProductRepository products;
    private final UomApplicationService uoms;
    private final SalesOrderQtyWriter qtyWriter;

    SalesTimesheetBillingServiceImpl(SalesOrderRepository orders, ProductRepository products,
                                     UomApplicationService uoms, SalesOrderQtyWriter qtyWriter) {
        this.orders = orders;
        this.products = products;
        this.uoms = uoms;
        this.qtyWriter = qtyWriter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalesTimesheetLineResponse> find(UUID companyId, UUID lineId) {
        return orders.findOrderIdByLineId(lineId).flatMap(orders::findByIdWithLines)
                .filter(o -> o.getCompanyId().equals(companyId))
                .flatMap(o -> o.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                        .map(l -> toResponse(o, l)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SalesTimesheetLineResponse> findAll(UUID companyId, Collection<UUID> lineIds) {
        Map<UUID, SalesTimesheetLineResponse> out = new LinkedHashMap<>();
        for (UUID id : lineIds) {
            find(companyId, id).ifPresent(r -> out.put(id, r));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTimesheetLineResponse> listEligible(UUID companyId, UUID customerPartnerId) {
        List<SalesTimesheetLineResponse> out = new ArrayList<>();
        for (SalesOrder o : orders.search(companyId, SalesOrderState.CONFIRMED, customerPartnerId, "", PageRequest.of(0, ORDER_PAGE))) {
            if (o.isLocked()) {
                continue;
            }
            SalesOrder full = orders.findByIdWithLines(o.getId()).orElse(o);
            for (SalesOrderLine l : full.getLines()) {
                SalesTimesheetLineResponse r = toResponse(full, l);
                if (r.eligible()) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    @Override
    @Transactional
    public void setDeliveredFromTimesheet(UUID companyId, UUID lineId, BigDecimal qty, String source) {
        UUID orderId = orders.findOrderIdByLineId(lineId).orElseThrow(() -> new SalesDomainException(
                "error.sales.lineNotFound", null, "Sales order line not found"));
        SalesOrder order = orders.findByIdWithLines(orderId).filter(o -> o.getCompanyId().equals(companyId))
                .orElseThrow(() -> new SalesDomainException("error.sales.lineNotFound", null, "Sales order line not found"));
        SalesOrderLine line = order.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst().orElseThrow();
        SalesTimesheetLineResponse info = toResponse(order, line);
        if (!info.eligible()) {
            throw new SalesDomainException("error.sales.notTimesheetLine", null,
                    "This line is not a service line invoiced from timesheets");
        }
        if (info.closed()) {
            throw new SalesDomainException("error.sales.orderClosed", null, "The order is canceled or locked");
        }
        if (qty == null || qty.signum() < 0) {
            throw new SalesDomainException("error.sales.timesheetQtyInvalid", null, "The quantity cannot be negative");
        }
        if (qty.compareTo(info.qtyInvoiced()) < 0) {
            throw new SalesDomainException("error.sales.timesheetBelowInvoiced", new Object[]{info.qtyInvoiced()},
                    "The delivered quantity cannot fall below what is already invoiced (" + info.qtyInvoiced().toPlainString() + ")");
        }
        qtyWriter.applyDeliveredFromTimesheet(orderId, lineId, qty, source);
    }

    private SalesTimesheetLineResponse toResponse(SalesOrder o, SalesOrderLine l) {
        boolean service = products.findById(new ProductId(l.getProductId()))
                .map(p -> p.getProductType() == ProductType.SERVICE).orElse(false);
        boolean eligible = service && l.getInvoicePolicy() == SalInvoicePolicy.TIMESHEET;
        boolean closed = o.getState() == SalesOrderState.CANCELLED || o.isLocked();
        return new SalesTimesheetLineResponse(l.getId(), o.getId(), o.getName(), o.getCustomerPartnerId(), l.getName(),
                unitOf(l), eligible, closed, nz(l.getQtyOrdered()), nz(l.getQtyDelivered()), nz(l.getQtyInvoiced()));
    }

    /** A line counted in days (by its unit's name) converts minutes with minutes-per-day; anything else is hours. */
    private String unitOf(SalesOrderLine l) {
        if (l.getUomId() == null) {
            return "HOURS";
        }
        try {
            String name = uoms.getUom(l.getUomId()).getName();
            String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
            return n.contains("day") || n.contains("يوم") ? "DAYS" : "HOURS";
        } catch (RuntimeException e) {
            return "HOURS";
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
