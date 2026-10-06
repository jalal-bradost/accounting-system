package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.dataaccess.entity.StockHoldEntity;
import com.bradox.erp.inventory.dataaccess.entity.StockHoldLockEntity;
import com.bradox.erp.inventory.dataaccess.repository.StockHoldJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockHoldLockJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockQuantJpaRepository;
import com.bradox.erp.inventory.domain.core.valueobject.LocationType;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockHoldRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockHoldRepositoryImpl implements StockHoldRepository {

    private final StockHoldJpaRepository holdJpa;
    private final StockHoldLockJpaRepository lockJpa;
    private final StockQuantJpaRepository quantJpa;

    public StockHoldRepositoryImpl(StockHoldJpaRepository holdJpa,
                                   StockHoldLockJpaRepository lockJpa,
                                   StockQuantJpaRepository quantJpa) {
        this.holdJpa = holdJpa;
        this.lockJpa = lockJpa;
        this.quantJpa = quantJpa;
    }

    @Override
    public List<HoldRow> findByOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId) {
        return holdJpa.findByOwner(companyId.getId(), ownerType, ownerId).stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    public Optional<HoldRow> findByOwnerAndProduct(CompanyId companyId,
                                                   StockHoldOwnerType ownerType,
                                                   UUID ownerId,
                                                   UUID productId) {
        return holdJpa.findByOwnerAndProduct(companyId.getId(), ownerType, ownerId, productId)
                .map(this::toRow);
    }

    @Override
    public HoldRow save(HoldRow row) {
        StockHoldEntity existing = row.id() != null
                ? holdJpa.findById(row.id()).orElse(null)
                : null;
        StockHoldEntity e = existing != null ? existing : new StockHoldEntity();
        if (e.getId() == null) {
            e.setId(row.id() != null ? row.id() : UUID.randomUUID());
            e.setCreatedAt(row.createdAt() != null ? row.createdAt() : Instant.now());
        }
        e.setCompanyId(row.companyId());
        e.setWarehouseId(row.warehouseId());
        e.setProductId(row.productId());
        e.setQuantity(row.quantity());
        e.setHolderId(row.holderId());
        e.setOwnerType(row.ownerType());
        e.setOwnerId(row.ownerId());
        e.setExpiresAt(row.expiresAt());
        e.setUpdatedAt(row.updatedAt() != null ? row.updatedAt() : Instant.now());
        return toRow(holdJpa.save(e));
    }

    @Override
    public void deleteByOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId) {
        holdJpa.deleteByOwner(companyId.getId(), ownerType, ownerId);
    }

    @Override
    public void delete(HoldRow row) {
        if (row.id() != null) {
            holdJpa.deleteById(row.id());
        }
    }

    @Override
    public BigDecimal sumActiveExcludingOwner(CompanyId companyId,
                                              WarehouseId warehouseId,
                                              UUID productId,
                                              Instant now,
                                              StockHoldOwnerType excludeOwnerType,
                                              UUID excludeOwnerId) {
        BigDecimal v = holdJpa.sumActiveExcludingOwner(
                companyId.getId(), warehouseId.getId(), productId, now, excludeOwnerType, excludeOwnerId);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumActive(CompanyId companyId, WarehouseId warehouseId, UUID productId, Instant now) {
        BigDecimal v = holdJpa.sumActive(companyId.getId(), warehouseId.getId(), productId, now);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumActiveExceptSalesOrder(CompanyId companyId,
                                                WarehouseId warehouseId,
                                                UUID productId,
                                                Instant now,
                                                UUID salesOrderId) {
        BigDecimal v = holdJpa.sumActiveExceptSalesOrder(
                companyId.getId(), warehouseId.getId(), productId, now,
                StockHoldOwnerType.SALES_ORDER, salesOrderId);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public Map<UUID, BigDecimal> sumActiveGroupedByProduct(CompanyId companyId,
                                                           WarehouseId warehouseId,
                                                           Instant now,
                                                           StockHoldOwnerType excludeOwnerType,
                                                           UUID excludeOwnerId) {
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (Object[] row : holdJpa.sumActiveGroupedByProduct(
                companyId.getId(), warehouseId.getId(), now, excludeOwnerType, excludeOwnerId)) {
            if (row == null || row[0] == null) continue;
            out.put((UUID) row[0], row[1] instanceof BigDecimal bd ? bd : BigDecimal.ZERO);
        }
        return out;
    }

    @Override
    @Transactional
    public void acquireProductLock(CompanyId companyId, WarehouseId warehouseId, UUID productId) {
        UUID c = companyId.getId();
        UUID w = warehouseId.getId();
        Optional<StockHoldLockEntity> existing = lockJpa.findForUpdate(c, w, productId);
        if (existing.isPresent()) return;
        try {
            lockJpa.saveAndFlush(new StockHoldLockEntity(c, w, productId));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent insert won — fall through to lock
        }
        lockJpa.findForUpdate(c, w, productId)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire stock hold lock"));
    }

    @Override
    public void lockWarehouseQuants(CompanyId companyId, WarehouseId warehouseId, Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        quantJpa.lockByWarehouseProducts(
                companyId.getId(), warehouseId.getId(), productIds, LocationType.INTERNAL);
    }

    @Override
    public BigDecimal sumFreeByWarehouse(CompanyId companyId, WarehouseId warehouseId, UUID productId) {
        BigDecimal v = quantJpa.sumFreeByWarehouse(
                companyId.getId(), productId, warehouseId.getId(), LocationType.INTERNAL);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public Map<UUID, BigDecimal[]> sumOnHandAndReservedByWarehouse(CompanyId companyId, WarehouseId warehouseId) {
        Map<UUID, BigDecimal[]> out = new HashMap<>();
        for (Object[] row : quantJpa.sumOnHandAndReservedGroupedByWarehouse(
                companyId.getId(), warehouseId.getId(), LocationType.INTERNAL)) {
            if (row == null || row[0] == null) continue;
            BigDecimal onHand = row[1] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            BigDecimal reserved = row[2] instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            out.put((UUID) row[0], new BigDecimal[]{onHand, reserved});
        }
        return out;
    }

    @Override
    public int deleteExpired(Instant now) {
        return holdJpa.deleteExpired(now);
    }

    private HoldRow toRow(StockHoldEntity e) {
        return new HoldRow(
                e.getId(),
                e.getCompanyId(),
                e.getWarehouseId(),
                e.getProductId(),
                e.getQuantity(),
                e.getHolderId(),
                e.getOwnerType(),
                e.getOwnerId(),
                e.getExpiresAt(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion());
    }
}
