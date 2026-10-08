package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.domain.core.valueobject.LineStatus;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.UpdateLineCommand;
import com.bradox.erp.repair.service.domain.ports.output.ProductLookupPort;
import com.bradox.erp.repair.service.domain.ports.output.repository.InspectionRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.LaborRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.LineRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.PackageRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LineServiceTest {

    static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    final UUID order = UUID.randomUUID();
    final Set<String> permissions = new HashSet<>(Set.of(RepairPermissions.ORDER_VIEW, RepairPermissions.LINE_EDIT,
            RepairPermissions.PRICE_VIEW));
    final Map<UUID, RepairLine> store = new HashMap<>();

    LaborRepository labor;
    LineApplicationServiceImpl service;
    final UUID categoryId = UUID.randomUUID();
    final UUID guideId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        LineRepository lines = mock(LineRepository.class);
        when(lines.save(any())).thenAnswer(i -> {
            RepairLine l = i.getArgument(0);
            store.put(l.id(), l);
            return l;
        });
        when(lines.find(any(), any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(1))));
        when(lines.listByOrder(any(), any())).thenAnswer(i -> store.values().stream().toList());

        labor = mock(LaborRepository.class);
        when(labor.findGuide(any(), any())).thenReturn(Optional.of(new GuideEntry(guideId, COMPANY.getId(), "BRK-01", "Replace front pads",
                null, null, categoryId, 90, null, null, null, null, true)));
        when(labor.findCategory(any(), any())).thenReturn(Optional.of(new LaborCategory(categoryId, COMPANY.getId(), "Mechanical",
                BigDecimal.valueOf(40), true)));

        SettingsRepository settings = mock(SettingsRepository.class);
        when(settings.find(any())).thenReturn(Optional.of(Settings.defaults(COMPANY.getId())));
        ProductLookupPort products = mock(ProductLookupPort.class);
        when(products.find(any(), any())).thenAnswer(i -> Optional.of(new ProductLookupPort.Product(i.getArgument(1), "Brake pad set",
                BigDecimal.valueOf(55))));

        com.bradox.erp.platform.security.AuthorizationPort auth = mock(com.bradox.erp.platform.security.AuthorizationPort.class);
        when(auth.hasAll(any(), any())).thenAnswer(i -> permissions.containsAll(i.<Set<String>>getArgument(1)));
        com.bradox.erp.platform.web.CompanyContext ctx = mock(com.bradox.erp.platform.web.CompanyContext.class);
        when(ctx.currentUser()).thenReturn(Optional.of(new com.bradox.erp.domain.valueobject.UserId(UUID.randomUUID())));
        when(ctx.currentUserDisplay()).thenReturn("advisor");
        RepairAccess access = new RepairAccess(ctx, auth, Clock.systemUTC());

        RepairSupport support = new RepairSupport(settings, labor, mock(InspectionRepository.class));
        LineFactory factory = new LineFactory(lines, labor, products, access);
        service = new LineApplicationServiceImpl(lines, mock(PackageRepository.class), factory, support, access, mock(AuditLogPort.class));
    }

    private AddLineCommand op(UUID guide, BigDecimal price) {
        return new AddLineCommand(LineType.OPERATION, null, null, null, null, price, null, guide, null, null, null, null, null);
    }

    @Test
    void operationFromGuideIsPricedFlatRateAndSnapshotsMinutesAndRate() {
        // Settings default hourly rate is null, so the category rate (40/h) applies: 90 min = 60.00
        LineResponse r = service.add(COMPANY, order, op(guideId, null));
        assertEquals(new BigDecimal("60.00"), r.unitPrice());
        assertEquals(90, r.standardMinutes());
        assertEquals(BigDecimal.valueOf(40), r.laborRate());
        assertEquals("Replace front pads", r.description());
        assertEquals("FLAT_RATE", r.pricingMode());
        assertFalse(r.priceMissing());
    }

    @Test
    void typedPriceOverridesGuidePrice() {
        assertEquals(BigDecimal.valueOf(80), service.add(COMPANY, order, op(guideId, BigDecimal.valueOf(80))).unitPrice());
    }

    @Test
    void noRateLeavesPriceMissing() {
        when(labor.findCategory(any(), any())).thenReturn(Optional.of(new LaborCategory(categoryId, COMPANY.getId(), "Mechanical", null, true)));
        assertTrue(service.add(COMPANY, order, op(guideId, null)).priceMissing());
    }

    @Test
    void partTakesNameAndListPriceFromProduct() {
        LineResponse r = service.add(COMPANY, order, new AddLineCommand(LineType.PART, null, UUID.randomUUID(), null,
                BigDecimal.valueOf(2), null, null, null, null, null, null, null, null));
        assertEquals("Brake pad set", r.description());
        assertEquals(BigDecimal.valueOf(55), r.unitPrice());
        assertEquals(new BigDecimal("110.00"), r.total());
    }

    @Test
    void customerPartAlwaysHasNoPrice() {
        LineResponse r = service.add(COMPANY, order, new AddLineCommand(LineType.CUSTOMER_PART, null, null, "Customer's filter",
                BigDecimal.ONE, BigDecimal.valueOf(30), BigDecimal.TEN, null, null, null, null, null, null));
        assertEquals(0, r.unitPrice().signum());
        assertEquals(0, r.discountPercent().signum());
    }

    @Test
    void discountAboveAdvisorLimitIsHeldUntilManagerApproves() {
        LineResponse r = service.add(COMPANY, order, new AddLineCommand(LineType.PART, null, null, "Oil", BigDecimal.ONE,
                BigDecimal.valueOf(20), BigDecimal.valueOf(50), null, null, null, null, null, null));
        assertTrue(r.needsDiscountApproval());
        assertThrows(ForbiddenException.class, () -> service.approveDiscount(COMPANY, r.id()));
        permissions.add(RepairPermissions.DISCOUNT_APPROVE);
        assertFalse(service.approveDiscount(COMPANY, r.id()).needsDiscountApproval());
    }

    @Test
    void pricesAreOmittedWithoutPriceView() {
        service.add(COMPANY, order, op(guideId, null));
        permissions.remove(RepairPermissions.PRICE_VIEW);
        var list = service.list(COMPANY, order);
        assertFalse(list.pricesVisible());
        assertNull(list.total());
        assertNull(list.lines().get(0).unitPrice());
        assertNull(list.lines().get(0).laborRate());
    }

    @Test
    void lineInAQuotationCannotBeEditedOrDeleted() {
        LineResponse r = service.add(COMPANY, order, op(guideId, null));
        RepairLine quoted = store.get(r.id());
        store.put(r.id(), new RepairLine(quoted.id(), quoted.companyId(), quoted.orderId(), quoted.type(), quoted.sectionLabel(),
                quoted.sequence(), quoted.productId(), quoted.description(), quoted.qty(), quoted.unitPrice(), quoted.discountPercent(),
                LineStatus.QUOTED, false, null, null, quoted.laborGuideId(), quoted.pricingMode(), quoted.standardMinutes(),
                quoted.laborRate(), null, null, null, null, null, quoted.createdAt(), quoted.createdBy()));
        assertThrows(RepairDomainException.class, () -> service.update(COMPANY, r.id(),
                new UpdateLineCommand(null, null, null, BigDecimal.TEN, null, null, null, null)));
        assertThrows(RepairDomainException.class, () -> service.delete(COMPANY, r.id()));
    }

    @Test
    void editingRequiresLineEditPermission() {
        permissions.remove(RepairPermissions.LINE_EDIT);
        assertThrows(ForbiddenException.class, () -> service.add(COMPANY, order, op(guideId, null)));
    }
}
