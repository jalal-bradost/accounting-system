# Accounting Base System – 5 Sessions Plan

This document splits the implementation into 5 sessions. Each session is self-contained and can be validated before moving to the next.

---

## Session 1: Structure and Hexagonal Boundaries (DONE)

**Goal:** Align the project with the reference structure (modular monolith like the microservice reference) and introduce ports, adapters, and container.

**Done:**
- Added `accounting-container` module (Spring Boot app, `BeanConfiguration`, `application.yml`).
- Added output ports in `accounting-application-service`: `AccountRepository`, `JournalRepository`, `JournalEntryRepository`.
- In `accounting-dataaccess`: adapters (`AccountRepositoryImpl`, `JournalRepositoryImpl`, `JournalEntryRepositoryImpl`), mappers (`AccountDataAccessMapper`, `JournalDataAccessMapper`, `JournalEntryDataAccessMapper`), JPA repositories (`AccountJpaRepository`, etc.).
- JPA entities: added `currency_code` to `JournalEntryEntity`; `JournalItemEntity` uses `BigDecimal` for `amount_currency` and has `label`; explicit getters/setters (no Lombok) for reliable build.
- Domain: getters on `Account`/`Journal`; `Account.builder()` / `Journal.builder()` for mappers.
- Container wires domain service, scans packages, `@EntityScan` and `@EnableJpaRepositories` for dataaccess.

**Deliverables (reference):**
- Add `accounting-container` module (Spring Boot app, `BeanConfiguration`).
- Rename/use `accounting-application-domain` as `accounting-application-service`; add **output ports**: `AccountRepository`, `JournalRepository`, `JournalEntryRepository` (interfaces in application-service).
- In `accounting-dataaccess`: **adapters** implementing the ports, **mappers** (domain ↔ JPA entity), **JPA repositories** (Spring Data).
- Persistence: add `currency_code` to `JournalEntryEntity`; make `JournalItemEntity` persistable (e.g. `BigDecimal` for amount_currency).
- Wire container to depend on application, dataaccess, application-service, domain-core, messaging.

**Reference:** Same layering as `customer-service` in Microservice-Architecture-master (container, application-service with ports, dataaccess with adapter + mapper).

---

## Session 2: Application Services and REST API (DONE)

**Goal:** Implement use cases and expose them via REST.

**Done:**
- **Input ports:** `AccountApplicationService`, `JournalApplicationService`, `JournalEntryApplicationService` in `accounting-application-service` (create, get, list where applicable; post, reverse for journal entry).
- **Commands/responses:** CreateAccountCommand/Response, CreateJournalCommand/Response, CreateJournalEntryCommand/Response, JournalItemCommand, ReverseJournalEntryCommand/Response; AccountResponse, JournalResponse, JournalEntryResponse (with JournalItemResponse).
- **Command handlers:** CreateAccountCommandHandler, CreateJournalCommandHandler, CreateJournalEntryCommandHandler, PostJournalEntryCommandHandler, ReverseJournalEntryCommandHandler (use repositories + domain service + mapper).
- **Application service impl:** AccountApplicationServiceImpl, JournalApplicationServiceImpl, JournalEntryApplicationServiceImpl delegate to handlers and expose get/list via repositories + mapper.
- **REST:** `AccountController` (POST, GET /{id}, GET ?companyId=), `JournalController` (same), `JournalEntryController` (GET /{id}, POST create, POST /{id}/post, POST /{id}/reverse with optional body { "reason": "..." }).
- **Exception handling:** `AccountingExceptionHandler` in `accounting-application` maps `AccountingDomainException` to 422 and `IllegalArgumentException` to 400.
- **Mapper:** `AccountingDataMapper` (command→domain, domain→response for accounts, journals, journal entries).

---

## Session 3: Double-Entry Reliability (DONE)

**Goal:** Harden double-entry rules and immutability.

**Done:**
- **Single scale:** `MonetaryScale` in common-domain (SCALE=4, HALF_EVEN); `Money` uses it in constructor and in add/subtract/multiply; `AccountingDataMapper.journalItemCommandsToDomain` scales debit/credit via `MonetaryScale.scale()`. DB already uses DECIMAL(19,4) for journal_items.
- **Item-level rules:** `JournalEntry.validate()` enforces: at least two lines; each line has exactly one of debit or credit > 0 (the other must be zero); then balance check (sum debit == sum credit).
- **Immutability:** In `JournalEntryRepositoryImpl.save()`, if existing entity has status POSTED, throw `AccountingDomainException` ("Cannot modify a posted journal entry. Use reversal instead.").
- **Idempotent post:** In `PostJournalEntryCommandHandler`, if `entry.getStatus() == POSTED` return success response without calling domain service or save.
- **Reversal integrity:** `JournalEntry` has optional `reversalOfEntryId` (JournalEntryId); `AccountingDomainService.createReversalEntry(original, reason, reversalSequenceNumber)` sets `sequenceNumber` and `reversalOfEntryId(original.getId())` on the new entry. Reversal handler generates `reversalSequenceNumber` as "REV-{originalSeq}-{timestamp}". `JournalEntryEntity` and mapper persist/load `reversal_of_entry_id`. `JournalEntryResponse` and API expose `reversalOfEntryId`.

---

## Session 4: Odoo-like Base (Sequence, Fiscal Period, Lock Date) (DONE)

**Goal:** Add sequence generation and period/lock controls.

**Done:**
- **SequenceGeneratorPort** (output port): `getNextSequenceNumber(CompanyId, JournalId, LocalDate)`. **SequenceGeneratorAdapter**: table `journal_entry_sequences` (company_id, journal_id, period_key e.g. year, last_number); pessimistic lock, increment, format e.g. `JOU-2025-00001`. CreateJournalEntry and ReverseJournalEntry use it; optional `sequenceNumber` in command (server generates when omitted).
- **CompanyLockDatePort** (output): `getPeriodLockDate(CompanyId)`, `setPeriodLockDate(CompanyId, LocalDate)`. **CompanyLockDateAdapter** + **CompanySettingsEntity** / **CompanySettingsJpaRepository** (table `company_settings`). **PostJournalEntryCommandHandler**: if entry date is before lock date → throw `AccountingDomainException`.
- **FiscalPeriodRepository** (output): `findPeriodContaining(CompanyId, LocalDate)` → `Optional<FiscalPeriodInfo>`, `create(...)`. **FiscalPeriodRepositoryAdapter** + **FiscalPeriodEntity** / **FiscalPeriodJpaRepository** (table `fiscal_periods`). Post handler: if a period contains the entry date and it is not open → throw.
- **CompanySettingsApplicationService** and **FiscalPeriodApplicationService** (input ports) with implementations. **REST:** PUT `/api/v1/companies/{companyId}/settings` (body `{ "periodLockDate": "yyyy-MM-dd" }`), POST `/api/v1/companies/{companyId}/fiscal-periods` (body `{ "startDate", "endDate", "open" }` → returns created period).

---

## Session 5: Account Balance, Reconciliation Stub, Audit (DONE)

**Goal:** Support reporting and reconciliation.

**Done:**
- **Account balance (computed):** **AccountBalanceRepository** (output port) with `getTrialBalance(CompanyId, from, to)`. **AccountBalanceRepositoryAdapter** + **JournalItemJpaRepository** JPQL. **ReportingApplicationService** + impl. REST: GET `/api/v1/companies/{companyId}/trial-balance?from=&to=` returns lines with accountId and balance.
- **Reconciliation stub:** **JournalItem** and **JournalItemEntity** have nullable `reconciliationId`. **JournalItemReconciliationPort** (output); **ReconciliationApplicationService** + impl. REST: POST `/api/v1/journal-items/reconcile`, POST `/api/v1/journal-items/unreconcile`. **JournalItemResponse** exposes `reconciliationId`.
- **Audit fields:** **JournalEntryEntity** and **JournalEntry** have `createdAt`, `updatedAt`, `postedAt`, `postedBy`; set in adapter on save; **JournalEntryResponse** exposes all four.

---

## Session 6: Platform Cross-Cutting (DONE)

**Goal:** Provide module-agnostic infrastructure for archiving, multi-company, audit, activity (chatter), and RBAC, so downstream modules can opt in by annotation.

**Done:**
- **`common-platform` module** with sibling JPA entities (`AuditLogEntity`, `ActivityMessageEntity`, `AppUserEntity`, `RoleEntity`, `PermissionEntity`, `UserRoleEntity`, `RolePermissionEntity`).
- **Soft delete:** `ArchivableAggregateRoot<ID>` (common-domain) and `ArchivableEntity` `@MappedSuperclass` (common-platform) with `active` + `archived_at` + `archived_by`.
- **Multi-company:** `CompanyContext` (singleton; values on `HttpServletRequest` attributes) populated by `CompanyContextFilter` from `X-Company-Id` header / `companyId` param; `@CurrentCompany` argument resolver.
- **Audit:** `AuditLogPort` / `AuditLogService` (`Propagation.REQUIRES_NEW`); JPA `AuditingEntityListener` driven by `@AuditableModel` / `@AuditTrack`; `AuditContextHolder` static bridge so the listener can reach the service even outside a request scope (seeders).
- **Activity (chatter):** `ActivityMessageEntity` + `ActivityApplicationService` and REST under `/api/v1/activities`.
- **RBAC:** `@RequiresPermission("...")` annotation + `PermissionAspect` (AOP) calling `AuthorizationPort`. Default `DefaultAuthorizationPortImpl` is permissive (open) for dev; replace with a real auth provider in prod.
- **Seeded permissions and roles:** `PlatformRbacSeeder` seeds the canonical permission catalogue and `ADMIN` / `ACCOUNTANT` / `SALES` / `PURCHASING` / `WAREHOUSE` / `READONLY` roles for the demo company.

---

## Session 7: Contacts (Customers / Vendors) (DONE)

**Goal:** Single `Partner` aggregate that can be customer, vendor, or both, with multi-address, payment terms, and bank accounts; integrate with accounting for receivable/payable balances and credit checks.

**Done:**
- **`contacts-service` Maven sub-modules** (`contacts-domain-core`, `contacts-application-service`, `contacts-application`, `contacts-dataaccess`) following the existing hexagonal pattern.
- **Domain:** `Partner` aggregate (company vs individual, isCustomer/isVendor flags, credit limit, payment terms ref, bank accounts, addresses), `PartnerAddress`, `PartnerBankAccount`, `PaymentTerms`. Soft-delete via `ArchivableAggregateRoot`.
- **Application:** `PartnerApplicationService` + handlers, `PaymentTermsApplicationService`, `CreditLimitChecker` domain service, `ContactsExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)`).
- **REST:** `/api/v1/contacts/partners` (CRUD, archive, search, addresses, bank accounts), `/api/v1/contacts/payment-terms` (CRUD), `/api/v1/contacts/partners/{id}/credit-status` (current AR balance + remaining credit limit).
- **Accounting integration:** `partnerId` / `partnerName` snapshot fields on `JournalEntry` and each `JournalItem` (domain, DTOs, JPA entity, mapper). New ports: `PartnerLookupPort` (resolve partner snapshot) and `PartnerBalancePort` (sum receivables/payables) with adapters in `accounting-container`.

---

## Session 8: Inventory (Products, Pickings, Valuation) (DONE)

**Goal:** Odoo-style stockable inventory with products, units of measure, warehouses & locations, stock pickings, on-hand quants, and append-only valuation layers, integrated with accounting for stock-IN / stock-OUT / COGS journal entries.

**Done:**
- **`inventory-service` Maven sub-modules** mirroring the contacts/accounting pattern (`inventory-domain-core`, `inventory-application-service`, `inventory-application`, `inventory-dataaccess`).
- **Domain entities:** `Product`, `ProductCategory`, `UnitOfMeasure` + `UomCategory` (with `REFERENCE` / `BIGGER` / `SMALLER` factor model), `Warehouse`, `StockLocation` (`INTERNAL` / `SUPPLIER` / `CUSTOMER` / `TRANSIT` / `INVENTORY_LOSS` / `PRODUCTION` / `VIEW`), `StockPicking` (with state machine `DRAFT` → `CONFIRMED` → `ASSIGNED` → `DONE` | `CANCELLED`), `StockMove`, `StockQuant` (with `@Version` for optimistic locking), `StockValuationLayer`.
- **Valuation strategies (pure domain):** `StandardCostStrategy`, `AvcoStrategy`, `FifoStrategy`, selected per-product or per-category via `ValuationStrategyFactory`. AVCO updates the running average on receipt; FIFO consumes oldest layers oldest-first; STANDARD uses fixed product cost.
- **Application services:** `ProductApplicationService`, `UomApplicationService`, `WarehouseApplicationService`, `StockPickingApplicationService` (orchestrates the full receive / deliver / internal-transfer / adjust pipeline including reservation, valuation, on-hand mutation, JE posting, and backorder creation), `StockValuationApplicationService` (on-hand and valuation queries).
- **REST:** `/api/v1/inventory/products`, `/product-categories`, `/uoms`, `/uom-categories`, `/warehouses`, `/stock-locations`, `/pickings` (`POST`, `/{id}/confirm`, `/assign`, `/validate`, `/cancel`, `/return`, `/adjust`), `/quants`, `/on-hand/{productId}`, `/valuation-layers`, `/valuation/{productId}`. `InventoryExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)`) maps `InventoryDomainException` → 422, `OptimisticLockingFailureException` → 409.
- **Accounting integration:** new `JournalEntryPostingPort` consumed from inventory; `JournalEntryPostingAdapter` in `accounting-container` routes inventory entries to the seeded `INV` journal. Receipts post `Dr Stock Valuation / Cr Stock Input`; deliveries post `Dr COGS / Cr Stock Valuation`. Account ids resolved per-product or per-category.
- **Bootstrap (`DefaultInventorySeeder`):** seeds UoM categories (Units / Weight / Length / Time) + reference units + common units, default warehouse `WH` with auto-provisioned `WH/STOCK` / `WH/INPUT` / `WH/OUTPUT` internal locations and the virtual `VIRT/SUPPLIERS` / `VIRT/CUSTOMERS` / `VIRT/INVENTORY-LOSS` locations, and a default product category `All` wired to seeded inventory accounts (`430010` Inventory, `430011` Stock Input, `430012` Stock Output, `430009` COGS).

---

## Session 9: Edge-Case Validation & Documentation (DONE)

**Goal:** End-to-end coverage of the major inventory flows and small fixes uncovered along the way.

**Done:**
- **`InventoryApiIntegrationTest`** (full Spring Boot context) covering:
  1. Receive / second receive (AVCO running average) / partial deliver / inventory adjustment.
  2. Partial validation that creates a backorder picking in `DRAFT`.
  3. Negative-stock delivery rejected with `INVENTORY_DOMAIN_ERROR` (422).
  4. No-op adjustment rejected with `INVENTORY_DOMAIN_ERROR` (422).
  5. UoM conversion endpoint (Dozen → Unit).
- **Bug fixes uncovered by the tests:**
  - `InventoryDataMapper.moveCommandToDomain` now propagates `unitCost` from the command (without it, AVCO/FIFO valued every receipt at zero).
  - `StockValuationLayerJpaRepository.sumOnHandValue` now sums `value` across all layers (positive receipts net negative deliveries) instead of only the un-consumed `remainingValue` of positive layers — fixes AVCO valuation drift.
  - `StockPickingApplicationServiceImpl.validatePicking` no longer adds/subtracts a synthetic delta to `onHandValue` before invoking the strategy (the SVL had not been written yet at that point, so the value was already pre-move).
  - `AuditContextHolder` and `AuditLogService` defensively handle the missing request scope so seeders no longer log audit warnings.
  - All module-specific exception handlers (`AccountingExceptionHandler`, `ContactsExceptionHandler`, `InventoryExceptionHandler`, `PlatformExceptionHandler`) are now annotated `@Order(HIGHEST_PRECEDENCE)` so the catch-all `GlobalExceptionHandler` no longer wins the resolution race.
  - `StockValuationLayerEntity.value` column renamed to `total_value` (avoids H2 reserved keyword conflict).

---

## Session Summary

| Session | Focus |
|--------|--------|
| 1 | Structure, container, ports, adapters, mappers |
| 2 | Application services, commands, REST API |
| 3 | Double-entry rules, scale, immutability, reversal link |
| 4 | Sequence, fiscal period, lock date |
| 5 | Balance query, reconciliation id, audit fields |
| 6 | Platform: archive, company context, audit, activity, RBAC |
| 7 | Contacts: partner / addresses / payment terms / accounting integration |
| 8 | Inventory: products, UoM, warehouses, pickings, valuation, accounting integration |
| 9 | Inventory edge-case integration tests + small fixes |
