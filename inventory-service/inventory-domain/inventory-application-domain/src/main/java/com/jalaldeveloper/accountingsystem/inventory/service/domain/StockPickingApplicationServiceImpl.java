package com.jalaldeveloper.accountingsystem.inventory.service.domain;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.event.StockPickingValidatedEvent;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockLocation;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockMove;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockPicking;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockQuant;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.service.ValuationContext;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.service.ValuationResult;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.service.ValuationStrategy;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.service.ValuationStrategyFactory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockQuantId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationLayerId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.CreateStockPickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.InventoryAdjustmentCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockMoveCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockPickingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValidatePickingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.mapper.InventoryDataMapper;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.StockPickingApplicationService;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.accounting.JournalEntryPostingPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.messaging.InventoryEventPublisher;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockLocationRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockPickingRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockQuantRepository;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.PurchaseReceiveSyncPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.SalesDeliverySyncPort;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.StockValuationLayerRepository;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditLogPort;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the picking lifecycle. Most business invariants live on the domain entities;
 * this service coordinates the side-effects: reservation, valuation, on-hand mutation,
 * journal posting, and backorder creation.
 */
@Service
@Validated
class StockPickingApplicationServiceImpl implements StockPickingApplicationService {

    private static final String MODEL_NAME = "inventory.stock.picking";

    private final StockPickingRepository pickingRepository;
    private final StockQuantRepository quantRepository;
    private final StockValuationLayerRepository layerRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final StockLocationRepository locationRepository;
    private final InventoryDataMapper mapper;
    private final ValuationStrategyFactory valuationFactory;
    private final ObjectProvider<JournalEntryPostingPort> journalPostingProvider;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final AuditLogPort auditLogPort;
    private final ObjectProvider<PurchaseReceiveSyncPort> purchaseReceiveSyncProvider;
    private final ObjectProvider<SalesDeliverySyncPort> salesDeliverySyncProvider;
    private final InventoryEventPublisher inventoryEventPublisher;

    StockPickingApplicationServiceImpl(StockPickingRepository pickingRepository,
                                       StockQuantRepository quantRepository,
                                       StockValuationLayerRepository layerRepository,
                                       ProductRepository productRepository,
                                       ProductCategoryRepository categoryRepository,
                                       StockLocationRepository locationRepository,
                                       InventoryDataMapper mapper,
                                       ValuationStrategyFactory valuationFactory,
                                       ObjectProvider<JournalEntryPostingPort> journalPostingProvider,
                                       ObjectProvider<CompanyContext> companyContextProvider,
                                       AuditLogPort auditLogPort,
                                       ObjectProvider<PurchaseReceiveSyncPort> purchaseReceiveSyncProvider,
                                       ObjectProvider<SalesDeliverySyncPort> salesDeliverySyncProvider,
                                       InventoryEventPublisher inventoryEventPublisher) {
        this.pickingRepository = pickingRepository;
        this.quantRepository = quantRepository;
        this.layerRepository = layerRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.mapper = mapper;
        this.valuationFactory = valuationFactory;
        this.journalPostingProvider = journalPostingProvider;
        this.companyContextProvider = companyContextProvider;
        this.auditLogPort = auditLogPort;
        this.purchaseReceiveSyncProvider = purchaseReceiveSyncProvider;
        this.salesDeliverySyncProvider = salesDeliverySyncProvider;
        this.inventoryEventPublisher = inventoryEventPublisher;
    }

    @Override
    @Transactional
    public StockPickingResponse createPicking(CreateStockPickingCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        StockLocation source = loadLocation(command.getSourceLocationId());
        StockLocation destination = loadLocation(command.getDestinationLocationId());
        validatePickingTypeMatchesLocations(command.getPickingType(), source, destination);

        UUID pickingId = UUID.randomUUID();
        List<StockMove> moves = new ArrayList<>();
        for (StockMoveCommand moveCmd : command.getMoves()) {
            StockMove m = mapper.moveCommandToDomain(moveCmd, UUID.randomUUID(),
                    source.getId(), destination.getId());
            moves.add(m);
        }
        StockPicking picking = mapper.pickingCommandToDomain(command, pickingId, companyId, moves);
        picking.validateInvariants();
        StockPicking saved = pickingRepository.save(picking);
        auditLogPort.recordBusinessEvent(companyId, MODEL_NAME, pickingId,
                "Picking created (" + saved.getPickingType() + ")", null);
        return mapper.pickingToResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse confirmPicking(UUID pickingId) {
        StockPicking p = loadOrThrow(pickingId);
        p.confirm();
        StockPicking saved = pickingRepository.save(p);
        return mapper.pickingToResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse assignPicking(UUID pickingId) {
        StockPicking p = loadOrThrow(pickingId);
        if (p.getState() != PickingState.CONFIRMED && p.getState() != PickingState.ASSIGNED) {
            throw new InventoryDomainException(
                    "Cannot assign picking in state " + p.getState() + " (must be CONFIRMED or ASSIGNED)");
        }
        // Only OUTGOING / INTERNAL reserve from internal source locations.
        StockLocation source = loadLocation(p.getSourceLocationId().getId());
        for (StockMove move : p.getMoves()) {
            if (move.getState() == MoveState.DONE || move.getState() == MoveState.CANCELLED) continue;
            BigDecimal demand = move.getDemandQuantity().subtract(move.getReservedQuantity()).max(BigDecimal.ZERO);
            if (demand.signum() <= 0) {
                move.markAssigned(move.getReservedQuantity());
                continue;
            }
            if (!source.isInternal()) {
                // Receipts (SUPPLIER -> INTERNAL) cannot reserve; treated as fully ASSIGNED.
                move.markAssigned(move.getDemandQuantity());
                continue;
            }
            Optional<StockQuant> quantOpt = quantRepository.findByProductLocation(
                    p.getCompanyId(), move.getProductId(), source.getId());
            BigDecimal available = quantOpt.map(StockQuant::getAvailable).orElse(BigDecimal.ZERO);
            BigDecimal toReserve = demand.min(available);
            if (toReserve.signum() > 0) {
                StockQuant quant = quantOpt.orElseGet(() -> ensureQuant(p.getCompanyId(), move.getProductId(), source.getId()));
                quant.reserve(toReserve);
                quantRepository.save(quant);
                move.markAssigned(move.getReservedQuantity().add(toReserve));
            } else {
                move.markAssigned(move.getReservedQuantity());
            }
        }
        p.recomputeState();
        return mapper.pickingToResponse(pickingRepository.save(p));
    }

    @Override
    @Transactional
    public StockPickingResponse validatePicking(UUID pickingId, ValidatePickingCommand command) {
        StockPicking picking = loadOrThrow(pickingId);
        if (picking.getState() == PickingState.DONE) {
            throw new InventoryDomainException("Picking already validated");
        }
        if (picking.getState() == PickingState.CANCELLED) {
            throw new InventoryDomainException("Cannot validate a cancelled picking");
        }
        if (picking.getState() == PickingState.DRAFT) {
            picking.confirm();
        }

        StockLocation source = loadLocation(picking.getSourceLocationId().getId());
        StockLocation destination = loadLocation(picking.getDestinationLocationId().getId());

        Map<UUID, BigDecimal> pickedOverrides = new HashMap<>();
        if (command != null && command.getPicks() != null) {
            for (ValidatePickingCommand.MovePicked mp : command.getPicks()) {
                pickedOverrides.put(mp.getMoveId(), mp.getPickedQuantity());
            }
        }

        List<JournalEntryPostingPort.JournalLine> journalLines = new ArrayList<>();
        List<StockValuationLayer> newLayers = new ArrayList<>();
        List<StockValuationLayer> updatedLayers = new ArrayList<>();
        List<StockMove> backorderCandidates = new ArrayList<>();

        for (StockMove move : picking.getMoves()) {
            if (move.getState() == MoveState.DONE || move.getState() == MoveState.CANCELLED) continue;
            BigDecimal picked = pickedOverrides.getOrDefault(
                    move.getId() != null ? move.getId().getId() : null,
                    move.getDemandQuantity());
            if (picked.signum() <= 0) {
                move.cancel();
                continue;
            }
            Product product = loadProduct(move.getProductId());
            ProductCategory category = product.getCategoryId() != null
                    ? categoryRepository.findById(product.getCategoryId()).orElse(null)
                    : null;
            boolean valued = product.isValued();

            // Apply quant deltas.
            if (source.isInternal()) {
                StockQuant src = ensureQuant(picking.getCompanyId(), move.getProductId(), source.getId());
                src.applyDelta(picked.negate(), source.isAllowNegativeStock());
                quantRepository.save(src);
            }
            if (destination.isInternal()) {
                StockQuant dst = ensureQuant(picking.getCompanyId(), move.getProductId(), destination.getId());
                dst.applyDelta(picked, destination.isAllowNegativeStock());
                quantRepository.save(dst);
            }

            // Compute valuation (only for STOCKABLE/VALUED products).
            ValuationResult result = null;
            if (valued) {
                ValuationMethod method = product.resolveValuationMethod(category);
                ValuationStrategy strategy = valuationFactory.forMethod(method);
                Money providedUnitCost = providedUnitCost(move, product, picking.getPickingType());
                BigDecimal onHandQty = quantRepository.sumOnHandInternal(picking.getCompanyId(), move.getProductId());
                Money onHandValue = layerRepository.sumOnHandValue(picking.getCompanyId(), move.getProductId());

                if (isReceiptForProduct(picking.getPickingType(), source, destination)) {
                    // Quant deltas were applied above; SVL has NOT been written yet. Roll
                    // qty back to BEFORE state for the strategy; value is already pre-move.
                    ValuationContext ctx = new ValuationContext(
                            onHandQty.subtract(picked), onHandValue,
                            picked, providedUnitCost, List.of());
                    result = strategy.valueIncoming(ctx);
                    if (result.newAverageCost() != null) {
                        product.changeStandardCost(result.newAverageCost());
                        productRepository.save(product);
                    }
                } else if (isDeliveryForProduct(picking.getPickingType(), source, destination)) {
                    List<StockValuationLayer> fifoCandidates = method == ValuationMethod.FIFO
                            ? layerRepository.findFifoCandidates(picking.getCompanyId(), move.getProductId())
                            : List.of();
                    // Same: roll qty back to BEFORE-move; value is already pre-move.
                    ValuationContext ctx = new ValuationContext(
                            onHandQty.add(picked), onHandValue,
                            picked.negate(), providedUnitCost, fifoCandidates);
                    result = strategy.valueOutgoing(ctx);
                    updatedLayers.addAll(result.consumedLayerUpdates());
                } else {
                    // Internal transfer: no valuation impact (qty moved between two internal locations).
                    result = null;
                }
            }

            move.markDone(picked, result != null ? result.unitCost() : Money.ZERO);

            if (result != null && valued) {
                BigDecimal layerQty = isReceiptForProduct(picking.getPickingType(), source, destination)
                        ? picked
                        : picked.negate();
                Money layerValue = isReceiptForProduct(picking.getPickingType(), source, destination)
                        ? result.totalValue()
                        : new Money(result.totalValue().getAmount().negate());
                StockValuationLayer layer = StockValuationLayer.builder()
                        .id(new ValuationLayerId(UUID.randomUUID()))
                        .companyId(picking.getCompanyId())
                        .productId(move.getProductId())
                        .stockMoveId(move.getId())
                        .method(product.resolveValuationMethod(category))
                        .occurredAt(Instant.now())
                        .quantity(layerQty)
                        .unitCost(result.unitCost())
                        .value(layerValue)
                        .remainingQuantity(layerQty.signum() > 0 ? layerQty : BigDecimal.ZERO)
                        .remainingValue(layerQty.signum() > 0 ? layerValue : Money.ZERO)
                        .build();
                layer.validate();
                newLayers.add(layer);

                addJournalLines(journalLines, picking.getPickingType(), product, category,
                        result.totalValue(), source, destination);
            }

            BigDecimal backorderQty = move.backorderQuantity();
            if (backorderQty.signum() > 0 && command != null && command.isCreateBackorder()) {
                StockMove bo = StockMove.builder()
                        .id(new com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId(UUID.randomUUID()))
                        .productId(move.getProductId())
                        .uomId(move.getUomId())
                        .sourceLocationId(move.getSourceLocationId())
                        .destinationLocationId(move.getDestinationLocationId())
                        .demandQuantity(backorderQty)
                        .purchaseOrderLineId(move.getPurchaseOrderLineId())
                        .salesOrderLineId(move.getSalesOrderLineId())
                        .build();
                backorderCandidates.add(bo);
            }
        }

        // Persist new + updated valuation layers.
        if (!updatedLayers.isEmpty()) {
            layerRepository.saveAll(updatedLayers);
        }
        Map<StockValuationLayer, StockValuationLayer> savedLayers = new HashMap<>();
        if (!newLayers.isEmpty()) {
            for (StockValuationLayer layer : newLayers) {
                savedLayers.put(layer, layerRepository.save(layer));
            }
        }

        // Post journal entry (if any valuation lines).
        UUID journalEntryId = null;
        if (!journalLines.isEmpty()) {
            JournalEntryPostingPort posting = journalPostingProvider.getIfAvailable();
            if (posting != null) {
                LocalDate entryDate = picking.getValidatedAt() != null
                        ? picking.getValidatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                        : LocalDate.now();
                journalEntryId = posting.postValuationEntry(
                        picking.getCompanyId(), entryDate,
                        picking.getReference() != null ? picking.getReference() : ("PICK-" + picking.getId().getId()),
                        picking.getPartnerId(), journalLines);
            }
        }
        if (journalEntryId != null) {
            for (StockValuationLayer saved : savedLayers.values()) {
                saved.linkJournalEntry(journalEntryId);
                layerRepository.save(saved);
            }
        }

        picking.markValidated(currentUserDisplay());
        StockPicking savedPicking = pickingRepository.save(picking);

        if (savedPicking.getPurchaseOrderId() != null
                && (savedPicking.getPickingType() == PickingType.INCOMING
                || savedPicking.getPickingType() == PickingType.OUTGOING)) {
            UUID poId = savedPicking.getPurchaseOrderId();
            PurchaseReceiveSyncPort sync = purchaseReceiveSyncProvider.getIfAvailable();
            if (sync != null) {
                sync.afterIncomingPickingValidated(poId);
            }
        }
        if (savedPicking.getSalesOrderId() != null
                && (savedPicking.getPickingType() == PickingType.OUTGOING
                || savedPicking.getPickingType() == PickingType.INCOMING)) {
            UUID soId = savedPicking.getSalesOrderId();
            SalesDeliverySyncPort salesSync = salesDeliverySyncProvider.getIfAvailable();
            if (salesSync != null) {
                salesSync.afterOutgoingPickingValidated(soId);
            }
        }
        inventoryEventPublisher.publishStockPickingValidated(new StockPickingValidatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                savedPicking.getCompanyId().getId(),
                savedPicking.getId().getId(),
                savedPicking.getPurchaseOrderId(),
                savedPicking.getSalesOrderId()));

        // Emit backorder picking if there are leftover quantities.
        if (!backorderCandidates.isEmpty()) {
            UUID backorderId = UUID.randomUUID();
            List<StockMove> rebuilt = backorderCandidates.stream()
                    .map(b -> StockMove.builder()
                            .id(b.getId())
                            .productId(b.getProductId())
                            .uomId(b.getUomId())
                            .sourceLocationId(b.getSourceLocationId())
                            .destinationLocationId(b.getDestinationLocationId())
                            .demandQuantity(b.getDemandQuantity())
                            .purchaseOrderLineId(b.getPurchaseOrderLineId())
                            .salesOrderLineId(b.getSalesOrderLineId())
                            .build())
                    .toList();
            StockPicking backorder = StockPicking.builder()
                    .id(new StockPickingId(backorderId))
                    .companyId(picking.getCompanyId())
                    .warehouseId(picking.getWarehouseId())
                    .pickingType(picking.getPickingType())
                    .reference(picking.getReference() != null ? picking.getReference() + "-BO" : null)
                    .sourceLocationId(picking.getSourceLocationId())
                    .destinationLocationId(picking.getDestinationLocationId())
                    .partnerId(picking.getPartnerId())
                    .origin(picking.getOrigin())
                    .purchaseOrderId(picking.getPurchaseOrderId())
                    .salesOrderId(picking.getSalesOrderId())
                    .moves(rebuilt)
                    .backorderOf(picking.getId())
                    .build();
            backorder.validateInvariants();
            pickingRepository.save(backorder);
        }

        auditLogPort.recordBusinessEvent(picking.getCompanyId(), MODEL_NAME, pickingId,
                "Picking validated", journalEntryId != null ? Map.of("journalEntryId", journalEntryId) : null);
        return mapper.pickingToResponse(savedPicking);
    }

    @Override
    @Transactional
    public StockPickingResponse cancelPicking(UUID pickingId) {
        StockPicking p = loadOrThrow(pickingId);
        // Release reservations.
        StockLocation source = loadLocation(p.getSourceLocationId().getId());
        if (source.isInternal()) {
            for (StockMove move : p.getMoves()) {
                if (move.getReservedQuantity().signum() > 0) {
                    quantRepository.findByProductLocation(p.getCompanyId(), move.getProductId(), source.getId())
                            .ifPresent(q -> {
                                q.release(move.getReservedQuantity());
                                quantRepository.save(q);
                            });
                }
            }
        }
        p.cancel();
        StockPicking saved = pickingRepository.save(p);
        auditLogPort.recordBusinessEvent(p.getCompanyId(), MODEL_NAME, pickingId,
                "Picking cancelled", null);
        return mapper.pickingToResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse returnPicking(UUID pickingId) {
        StockPicking original = loadOrThrow(pickingId);
        if (original.getState() != PickingState.DONE) {
            throw new InventoryDomainException("Only a DONE picking can be returned");
        }
        UUID newPickingId = UUID.randomUUID();
        List<StockMove> reversed = original.getMoves().stream()
                .filter(m -> m.getState() == MoveState.DONE)
                .map(m -> StockMove.builder()
                        .id(new com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId(UUID.randomUUID()))
                        .productId(m.getProductId())
                        .uomId(m.getUomId())
                        .sourceLocationId(m.getDestinationLocationId())
                        .destinationLocationId(m.getSourceLocationId())
                        .demandQuantity(m.getPickedQuantity())
                        .purchaseOrderLineId(m.getPurchaseOrderLineId())
                        .salesOrderLineId(m.getSalesOrderLineId())
                        .build())
                .collect(Collectors.toList());

        PickingType returnType = switch (original.getPickingType()) {
            case INCOMING -> PickingType.OUTGOING;
            case OUTGOING -> PickingType.INCOMING;
            case INTERNAL -> PickingType.INTERNAL;
        };

        StockPicking returnPicking = StockPicking.builder()
                .id(new StockPickingId(newPickingId))
                .companyId(original.getCompanyId())
                .warehouseId(original.getWarehouseId())
                .pickingType(returnType)
                .reference(original.getReference() != null ? original.getReference() + "-RET" : null)
                .sourceLocationId(original.getDestinationLocationId())
                .destinationLocationId(original.getSourceLocationId())
                .partnerId(original.getPartnerId())
                .purchaseOrderId(original.getPurchaseOrderId())
                .salesOrderId(original.getSalesOrderId())
                .origin("RETURN OF " + original.getId().getId())
                .moves(reversed)
                .backorderOf(original.getId())
                .build();
        returnPicking.validateInvariants();
        StockPicking saved = pickingRepository.save(returnPicking);
        auditLogPort.recordBusinessEvent(original.getCompanyId(), MODEL_NAME, newPickingId,
                "Return picking created", Map.of("returnOf", pickingId));
        return mapper.pickingToResponse(saved);
    }

    @Override
    @Transactional
    public StockPickingResponse adjustInventory(InventoryAdjustmentCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        StockLocation location = loadLocation(command.getLocationId());
        if (!location.isInternal()) {
            throw new InventoryDomainException("Adjustments only allowed at INTERNAL locations");
        }
        StockQuant quant = ensureQuant(companyId, new ProductId(command.getProductId()), location.getId());
        BigDecimal delta = command.getTargetQuantity().subtract(quant.getQuantity());
        if (delta.signum() == 0) {
            throw new InventoryDomainException(
                    "Inventory adjustment is a no-op (current qty already equals target " + command.getTargetQuantity() + ")");
        }

        // The counterparty location is a virtual INVENTORY_LOSS location that the bootstrap is
        // expected to provision per company. We delegate creation/lookup to the caller layer
        // (REST or seeder) — fall back to running the move through the same INTERNAL location if
        // not present (caller responsibility for completeness).
        StockLocation lossLocation = locationRepository.findByCompany(companyId, true).stream()
                .filter(l -> l.getLocationType() == LocationType.INVENTORY_LOSS)
                .findFirst()
                .orElseThrow(() -> new InventoryDomainException(
                        "No INVENTORY_LOSS location for company " + companyId.getId()
                                + " (bootstrap a virtual loss location to enable adjustments)"));

        // Build a one-line picking and delegate to validatePicking for the full pipeline.
        boolean isIncrease = delta.signum() > 0;
        CreateStockPickingCommand pickingCmd = new CreateStockPickingCommand();
        pickingCmd.setCompanyId(companyId.getId());
        pickingCmd.setPickingType(isIncrease ? PickingType.INCOMING : PickingType.OUTGOING);
        pickingCmd.setReference(command.getReason() != null ? command.getReason() : "ADJ");
        pickingCmd.setSourceLocationId(isIncrease ? lossLocation.getId().getId() : location.getId().getId());
        pickingCmd.setDestinationLocationId(isIncrease ? location.getId().getId() : lossLocation.getId().getId());
        StockMoveCommand move = new StockMoveCommand();
        move.setProductId(command.getProductId());
        Product product = loadProduct(new ProductId(command.getProductId()));
        move.setUomId(product.getUomId().getId());
        move.setDemandQuantity(delta.abs());
        move.setUnitCost(product.getStandardCost() != null ? product.getStandardCost().getAmount() : BigDecimal.ZERO);
        pickingCmd.setMoves(List.of(move));

        StockPickingResponse created = createPicking(pickingCmd);
        confirmPicking(created.getId());
        ValidatePickingCommand validate = new ValidatePickingCommand();
        validate.setCreateBackorder(false);
        return validatePicking(created.getId(), validate);
    }

    @Override
    @Transactional(readOnly = true)
    public StockPickingResponse getPicking(UUID pickingId) {
        return mapper.pickingToResponse(loadOrThrow(pickingId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockPickingResponse> searchPickings(CompanyId companyId, PickingType pickingType,
                                                      PickingState state, Pageable pageable) {
        return pickingRepository.search(companyId, pickingType, state, pageable)
                .map(mapper::pickingToResponse);
    }

    private StockPicking loadOrThrow(UUID id) {
        return pickingRepository.findById(new StockPickingId(id))
                .orElseThrow(() -> new InventoryDomainException("Picking not found: " + id));
    }

    private StockLocation loadLocation(UUID id) {
        return locationRepository.findById(new StockLocationId(id))
                .orElseThrow(() -> new InventoryDomainException("Location not found: " + id));
    }

    private Product loadProduct(ProductId id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new InventoryDomainException("Product not found: " + id.getId()));
    }

    private StockQuant ensureQuant(CompanyId companyId, ProductId productId, StockLocationId locationId) {
        return quantRepository.findByProductLocation(companyId, productId, locationId)
                .orElseGet(() -> quantRepository.save(StockQuant.builder()
                        .id(new StockQuantId(UUID.randomUUID()))
                        .companyId(companyId)
                        .productId(productId)
                        .locationId(locationId)
                        .quantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .lastChangedAt(Instant.now())
                        .build()));
    }

    private boolean isReceiptForProduct(PickingType type, StockLocation source, StockLocation dest) {
        return type == PickingType.INCOMING || (!source.isInternal() && dest.isInternal());
    }

    private boolean isDeliveryForProduct(PickingType type, StockLocation source, StockLocation dest) {
        return type == PickingType.OUTGOING || (source.isInternal() && !dest.isInternal());
    }

    private Money providedUnitCost(StockMove move, Product product, PickingType type) {
        if (move.getUnitCost() != null && move.getUnitCost().getAmount().signum() > 0) {
            return move.getUnitCost();
        }
        return product.getStandardCost() != null ? product.getStandardCost() : Money.ZERO;
    }

    private void addJournalLines(List<JournalEntryPostingPort.JournalLine> lines,
                                 PickingType type,
                                 Product product,
                                 ProductCategory category,
                                 Money value,
                                 StockLocation source,
                                 StockLocation destination) {
        UUID stockValuation = product.resolveStockValuationAccountId(category);
        UUID stockInput = product.resolveStockInputAccountId(category);
        UUID stockOutput = product.resolveStockOutputAccountId(category);
        UUID cogs = product.resolveCogsAccountId(category);

        if (isReceiptForProduct(type, source, destination)) {
            // Dr Stock Valuation, Cr Stock Input (a.k.a. GR/IR)
            require(stockValuation, "stock_valuation_account", product);
            require(stockInput, "stock_input_account", product);
            lines.add(new JournalEntryPostingPort.JournalLine(stockValuation,
                    "Stock IN " + product.getSku(), value, Money.ZERO));
            lines.add(new JournalEntryPostingPort.JournalLine(stockInput,
                    "Stock IN " + product.getSku(), Money.ZERO, value));
        } else if (isDeliveryForProduct(type, source, destination)) {
            // Dr COGS, Cr Stock Valuation
            require(stockValuation, "stock_valuation_account", product);
            require(cogs, "cogs_account", product);
            lines.add(new JournalEntryPostingPort.JournalLine(cogs,
                    "COGS " + product.getSku(), value, Money.ZERO));
            lines.add(new JournalEntryPostingPort.JournalLine(stockValuation,
                    "Stock OUT " + product.getSku(), Money.ZERO, value));
        } else {
            // Internal transfer: typically goods-in-transit posting; skipped for the MVP.
            // Caller can enable later if needed.
            return;
        }
        // suppress unused warning
        if (stockOutput == null) return;
    }

    private void require(UUID id, String label, Product product) {
        if (id == null) {
            throw new InventoryDomainException(
                    "Missing " + label + " for product " + product.getSku() + " (set on product or its category)");
        }
    }

    private void validatePickingTypeMatchesLocations(PickingType type,
                                                     StockLocation source,
                                                     StockLocation destination) {
        switch (type) {
            case INCOMING -> {
                if (source.isInternal() || !destination.isInternal()) {
                    throw new InventoryDomainException("INCOMING expects external source -> internal destination");
                }
            }
            case OUTGOING -> {
                if (!source.isInternal() || destination.isInternal()) {
                    throw new InventoryDomainException("OUTGOING expects internal source -> external destination");
                }
            }
            case INTERNAL -> {
                if (!source.isInternal() || !destination.isInternal()) {
                    throw new InventoryDomainException("INTERNAL expects both source and destination internal");
                }
            }
        }
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) return new CompanyId(explicit);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new IllegalArgumentException("companyId required (header X-Company-Id, query param, or body)"));
        }
        throw new IllegalArgumentException("companyId required");
    }

    private String currentUserDisplay() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }
}
