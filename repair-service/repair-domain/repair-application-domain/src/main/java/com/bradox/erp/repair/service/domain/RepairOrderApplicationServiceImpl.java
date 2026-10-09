package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.platform.document.DocumentSequenceService;
import com.bradox.erp.repair.domain.core.model.RepairOrder;
import com.bradox.erp.repair.domain.core.valueobject.RepairStatus;
import com.bradox.erp.repair.service.domain.dto.RepairOrderCommand;
import com.bradox.erp.repair.service.domain.dto.RepairOrderResponse;
import com.bradox.erp.repair.service.domain.ports.input.RepairOrderApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.RepairLookupPort;
import com.bradox.erp.repair.service.domain.ports.output.RepairOrderRepository;
import com.bradox.erp.repair.service.domain.ports.output.SaleQuotationPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
class RepairOrderApplicationServiceImpl implements RepairOrderApplicationService {

    private final RepairOrderRepository orders;
    private final RepairLookupPort lookup;
    private final SaleQuotationPort quotations;
    private final DocumentSequenceService sequences;
    private final RepairAccess access;
    private final RecordActivityLogger chatter;

    RepairOrderApplicationServiceImpl(RepairOrderRepository orders, RepairLookupPort lookup, SaleQuotationPort quotations,
                                      DocumentSequenceService sequences, RepairAccess access, RecordActivityLogger chatter) {
        this.orders = orders;
        this.lookup = lookup;
        this.quotations = quotations;
        this.sequences = sequences;
        this.access = access;
        this.chatter = chatter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrderResponse> list(CompanyId companyId) {
        access.require(RepairPermissions.ORDER_VIEW);
        return toResponses(companyId, orders.findAll(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderResponse get(CompanyId companyId, UUID id) {
        access.require(RepairPermissions.ORDER_VIEW);
        return toResponse(companyId, load(companyId, id));
    }

    @Override
    @Transactional
    public RepairOrderResponse create(CompanyId companyId, RepairOrderCommand c) {
        access.require(RepairPermissions.ORDER_EDIT);
        RepairOrder blank = new RepairOrder(UUID.randomUUID(), companyId.getId(), sequences.next(companyId.getId(), "RO"),
                null, null, null, false, RepairStatus.NEW, null, null, List.of(), access.now());
        RepairOrder saved = orders.save(apply(blank, c));
        log(saved, "Repair order created");
        return toResponse(companyId, saved);
    }

    @Override
    @Transactional
    public RepairOrderResponse update(CompanyId companyId, UUID id, RepairOrderCommand c) {
        access.require(RepairPermissions.ORDER_EDIT);
        return toResponse(companyId, orders.save(apply(load(companyId, id), c)));
    }

    @Override
    @Transactional
    public RepairOrderResponse confirm(CompanyId companyId, UUID id) {
        access.require(RepairPermissions.ORDER_EDIT);
        RepairOrder order = load(companyId, id);
        order.checkReadyToConfirm();
        SaleQuotationPort.Quotation quotation = null;
        if (!order.underWarranty()) {
            List<SaleQuotationPort.Line> lines = order.parts().stream()
                    .map(p -> new SaleQuotationPort.Line(p.productId(), p.qty())).toList();
            quotation = quotations.create(companyId, order.customerPartnerId(), "Repair " + order.reference(), lines);
        }
        RepairOrder confirmed = orders.save(order.confirm(quotation == null ? null : quotation.id(), quotation == null ? null : quotation.name()));
        log(confirmed, quotation == null ? "Repair confirmed (under warranty, no quotation)"
                : "Repair confirmed, quotation " + quotation.name() + " created");
        return toResponse(companyId, confirmed);
    }

    @Override
    @Transactional
    public RepairOrderResponse done(CompanyId companyId, UUID id) {
        access.require(RepairPermissions.ORDER_EDIT);
        RepairOrder done = orders.save(load(companyId, id).done());
        log(done, "Repair done");
        return toResponse(companyId, done);
    }

    @Override
    @Transactional
    public RepairOrderResponse cancel(CompanyId companyId, UUID id) {
        access.require(RepairPermissions.ORDER_EDIT);
        RepairOrder cancelled = orders.save(load(companyId, id).cancel());
        log(cancelled, "Repair cancelled");
        return toResponse(companyId, cancelled);
    }

    private static RepairOrder apply(RepairOrder order, RepairOrderCommand c) {
        List<RepairOrder.Part> parts = c.parts().stream()
                .map(p -> new RepairOrder.Part(UUID.randomUUID(), p.productId(), p.qty())).toList();
        return order.edit(c.customerPartnerId(), c.productId(), c.scheduledDate(), c.underWarranty(), parts);
    }

    private void log(RepairOrder order, String message) {
        chatter.log(order.companyId(), RecordActivityLogger.MODEL_REPAIR_ORDER, order.id(), message);
    }

    private RepairOrder load(CompanyId companyId, UUID id) {
        return orders.find(companyId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repair order not found"));
    }

    private RepairOrderResponse toResponse(CompanyId companyId, RepairOrder order) {
        return toResponses(companyId, List.of(order)).get(0);
    }

    private List<RepairOrderResponse> toResponses(CompanyId companyId, List<RepairOrder> list) {
        Set<UUID> partners = new HashSet<>();
        Set<UUID> products = new HashSet<>();
        for (RepairOrder o : list) {
            if (o.customerPartnerId() != null) {
                partners.add(o.customerPartnerId());
            }
            if (o.productId() != null) {
                products.add(o.productId());
            }
            o.parts().forEach(p -> products.add(p.productId()));
        }
        // HashMap: get(null) is allowed while an order has no customer or product yet.
        Map<UUID, String> partnerNames = new HashMap<>(partners.isEmpty() ? Map.of() : lookup.partnerNames(companyId, partners));
        Map<UUID, String> productNames = new HashMap<>(products.isEmpty() ? Map.of() : lookup.productNames(companyId, products));
        return list.stream().map(o -> new RepairOrderResponse(o.id(), o.reference(), o.customerPartnerId(),
                partnerNames.get(o.customerPartnerId()), o.productId(), productNames.get(o.productId()), o.scheduledDate(),
                o.underWarranty(), o.status(), o.saleOrderId(), o.saleOrderName(),
                o.parts().stream().map(p -> new RepairOrderResponse.Part(p.id(), p.productId(), productNames.get(p.productId()), p.qty())).toList(),
                o.createdAt())).toList();
    }
}
