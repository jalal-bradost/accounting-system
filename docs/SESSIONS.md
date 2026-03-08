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

## Session 4: Odoo-like Base (Sequence, Fiscal Period, Lock Date)

**Goal:** Add sequence generation and period/lock controls.

**Deliverables:**
- **SequenceGeneratorPort** (output port) and adapter: generate next journal entry number per journal/company (e.g. table or DB sequence).
- Use sequence in CreateJournalEntry and in reversal.
- **Fiscal period:** Value object or entity `FiscalPeriod` (start/end date, open/closed); optional `FiscalYear`; link `JournalEntry` to period (by date or stored).
- **Lock date:** Company (or settings) `periodLockDate`; in post use case reject if entry date is before lock date.
- Repository/port for period and company settings if needed.

---

## Session 5: Account Balance, Reconciliation Stub, Audit

**Goal:** Support reporting and reconciliation.

**Deliverables:**
- **Account balance (computed):** Query sum(debit)-sum(credit) per account from posted journal items; port/use case for “trial balance” or “ledger” by period/company.
- **Reconciliation stub:** Add `reconciliationId` (nullable) on `JournalItem` (domain + entity); use case “reconcile” (set same id on selected items), “unreconcile” (clear id).
- **Audit fields:** `createdAt`/`updatedAt` on entries; optional `postedAt`/`postedBy` (persistence or domain).

---

## Session Summary

| Session | Focus |
|--------|--------|
| 1 | Structure, container, ports, adapters, mappers |
| 2 | Application services, commands, REST API |
| 3 | Double-entry rules, scale, immutability, reversal link |
| 4 | Sequence, fiscal period, lock date |
| 5 | Balance query, reconciliation id, audit fields |
