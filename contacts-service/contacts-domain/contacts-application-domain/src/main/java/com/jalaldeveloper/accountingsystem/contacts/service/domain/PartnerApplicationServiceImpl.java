package com.jalaldeveloper.accountingsystem.contacts.service.domain;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.Partner;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerAddress;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PartnerBankAccount;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.service.CreditLimitChecker;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PartnerId;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.*;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.event.PartnerUpdatedEvent;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.mapper.ContactsDataMapper;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PartnerApplicationService;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.accounting.PartnerBalancePort;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.messaging.ContactsEventPublisher;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository.PartnerRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditLogPort;
import com.jalaldeveloper.accountingsystem.platform.activity.ActivityApplicationService;
import com.jalaldeveloper.accountingsystem.platform.activity.ActivityKind;
import com.jalaldeveloper.accountingsystem.platform.activity.CreateActivityCommand;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class PartnerApplicationServiceImpl implements PartnerApplicationService {

    private static final String MODEL_NAME = "contacts.partner";

    private final PartnerRepository partnerRepository;
    private final ContactsDataMapper mapper;
    private final ObjectProvider<PartnerBalancePort> partnerBalancePortProvider;
    private final ObjectProvider<CompanyContext> companyContextProvider;
    private final AuditLogPort auditLogPort;
    private final ContactsEventPublisher contactsEventPublisher;
    private final ActivityApplicationService activityService;
    private final com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.storage.PartnerImageStoragePort imageStorage;

    PartnerApplicationServiceImpl(PartnerRepository partnerRepository,
                                  ContactsDataMapper mapper,
                                  ObjectProvider<PartnerBalancePort> partnerBalancePortProvider,
                                  ObjectProvider<CompanyContext> companyContextProvider,
                                  AuditLogPort auditLogPort,
                                  ContactsEventPublisher contactsEventPublisher,
                                  ActivityApplicationService activityService,
                                  com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.storage.PartnerImageStoragePort imageStorage) {
        this.partnerRepository = partnerRepository;
        this.mapper = mapper;
        this.partnerBalancePortProvider = partnerBalancePortProvider;
        this.companyContextProvider = companyContextProvider;
        this.auditLogPort = auditLogPort;
        this.contactsEventPublisher = contactsEventPublisher;
        this.activityService = activityService;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public PartnerResponse createPartner(CreatePartnerCommand command) {
        CompanyId companyId = resolveCompany(command.getCompanyId());
        UUID id = UUID.randomUUID();
        Partner partner = mapper.createCommandToPartner(command, id, companyId);
        partner.validate();
        Partner saved = partnerRepository.save(partner);
        auditLogPort.recordBusinessEvent(companyId, MODEL_NAME, id,
                "Partner created: " + saved.getDisplayName(), null);
        recordSystemActivity(companyId, id, "Contact created");
        publishPartnerUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PartnerResponse uploadPartnerImage(UUID partnerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ContactsDomainException("Image file is required");
        }
        Partner partner = loadIncludingArchivedOrThrow(partnerId);
        partnerRepository.findImageMeta(partnerId).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.storage.PartnerImageStoragePort.StoredImage stored;
        try {
            stored = imageStorage.store(
                    partner.getCompanyId().getId(),
                    partnerId,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream());
        } catch (IOException ex) {
            throw new ContactsDomainException("Failed to read uploaded image");
        }
        partnerRepository.updateImage(partnerId, stored.publicUrl(), stored.contentType());
        auditLogPort.recordBusinessEvent(partner.getCompanyId(), MODEL_NAME, partnerId, "Partner image updated", null);
        return getPartner(partnerId);
    }

    @Override
    @Transactional
    public PartnerResponse deletePartnerImage(UUID partnerId) {
        Partner partner = loadIncludingArchivedOrThrow(partnerId);
        partnerRepository.findImageMeta(partnerId).ifPresent(meta -> imageStorage.deleteIfPresent(meta.imageUrl()));
        partnerRepository.clearImage(partnerId);
        auditLogPort.recordBusinessEvent(partner.getCompanyId(), MODEL_NAME, partnerId, "Partner image removed", null);
        return getPartner(partnerId);
    }

    @Override
    @Transactional
    public PartnerResponse updatePartner(UUID partnerId, UpdatePartnerCommand cmd) {
        Partner partner = loadOrThrow(partnerId);

        Map<String, Object> changes = new LinkedHashMap<>();
        if (cmd.getKind() != null && cmd.getKind() != partner.getKind()) {
            changes.put("kind", Map.of("old", partner.getKind(), "new", cmd.getKind()));
            partner.changeKind(cmd.getKind());
        }
        if (cmd.getDisplayName() != null || cmd.getLegalName() != null) {
            String newDisplay = cmd.getDisplayName() != null ? cmd.getDisplayName() : partner.getDisplayName();
            String newLegal = cmd.getLegalName() != null ? cmd.getLegalName() : partner.getLegalName();
            if (!newDisplay.equals(partner.getDisplayName())) {
                changes.put("displayName", Map.of("old", partner.getDisplayName(), "new", newDisplay));
            }
            partner.rename(newDisplay, newLegal);
        }
        if (Boolean.TRUE.equals(cmd.getParentIdReset())) {
            partner.changeParent(null);
        } else if (cmd.getParentId() != null) {
            partner.changeParent(new PartnerId(cmd.getParentId()));
        }
        if (cmd.getIsCustomer() != null && cmd.getIsCustomer() != partner.isCustomer()) {
            changes.put("isCustomer", Map.of("old", partner.isCustomer(), "new", cmd.getIsCustomer()));
            partner.setCustomer(cmd.getIsCustomer());
        }
        if (cmd.getIsVendor() != null && cmd.getIsVendor() != partner.isVendor()) {
            changes.put("isVendor", Map.of("old", partner.isVendor(), "new", cmd.getIsVendor()));
            partner.setVendor(cmd.getIsVendor());
        }
        if (cmd.getCreditLimit() != null) {
            Money oldLimit = partner.getCreditLimit();
            Money newLimit = new Money(cmd.getCreditLimit());
            if (!newLimit.equals(oldLimit)) {
                changes.put("creditLimit", Map.of("old", oldLimit.getAmount(), "new", newLimit.getAmount()));
                partner.changeCreditLimit(newLimit);
            }
        }
        if (Boolean.TRUE.equals(cmd.getPaymentTermsIdReset())) {
            partner.changePaymentTerms(null);
        } else if (cmd.getPaymentTermsId() != null) {
            partner.changePaymentTerms(new PaymentTermsId(cmd.getPaymentTermsId()));
        }
        if (Boolean.TRUE.equals(cmd.getReceivableAccountIdReset())) {
            partner.changeReceivableAccount(null);
        } else if (cmd.getReceivableAccountId() != null) {
            partner.changeReceivableAccount(cmd.getReceivableAccountId());
        }
        if (Boolean.TRUE.equals(cmd.getPayableAccountIdReset())) {
            partner.changePayableAccount(null);
        } else if (cmd.getPayableAccountId() != null) {
            partner.changePayableAccount(cmd.getPayableAccountId());
        }
        if (cmd.getTaxId() != null) partner.changeTaxId(cmd.getTaxId());
        if (cmd.getEmail() != null || cmd.getPhone() != null || cmd.getWebsite() != null) {
            partner.changeContact(
                    cmd.getEmail() != null ? cmd.getEmail() : partner.getEmail(),
                    cmd.getPhone() != null ? cmd.getPhone() : partner.getPhone(),
                    cmd.getWebsite() != null ? cmd.getWebsite() : partner.getWebsite());
        }
        if (cmd.getLanguage() != null) partner.changeLanguage(cmd.getLanguage());
        if (cmd.getCurrencyCode() != null && !cmd.getCurrencyCode().isBlank()) {
            partner.changeCurrency(new Currency(cmd.getCurrencyCode(), "", 2));
        }

        partner.validate();
        Partner saved = partnerRepository.save(partner);
        if (!changes.isEmpty()) {
            auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, partnerId,
                    "Partner updated", changes);
            publishPartnerUpdated(saved);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PartnerResponse archive(UUID partnerId) {
        Partner partner = loadOrThrow(partnerId);
        if (!partner.isActive()) {
            return toResponse(partner);
        }
        partner.archive(currentUserDisplay());
        Partner saved = partnerRepository.save(partner);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, partnerId,
                "Partner archived", null);
        recordSystemActivity(saved.getCompanyId(), partnerId, "Contact archived");
        publishPartnerUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PartnerResponse unarchive(UUID partnerId) {
        Partner partner = loadIncludingArchivedOrThrow(partnerId);
        if (partner.isActive()) {
            return toResponse(partner);
        }
        partner.unarchive();
        Partner saved = partnerRepository.save(partner);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, partnerId,
                "Partner unarchived", null);
        recordSystemActivity(saved.getCompanyId(), partnerId, "Contact unarchived");
        publishPartnerUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerResponse getPartner(UUID partnerId) {
        return toResponse(loadIncludingArchivedOrThrow(partnerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PartnerRefResponse> findRef(CompanyId companyId, UUID partnerId) {
        return partnerRepository.findByIdIncludingArchived(new PartnerId(partnerId))
                .filter(p -> companyId == null || p.getCompanyId().equals(companyId))
                .map(mapper::partnerToRef);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerResponse> search(CompanyId companyId, String query,
                                        Boolean isCustomer, Boolean isVendor,
                                        boolean includeArchived, Pageable pageable) {
        Page<PartnerResponse> page = partnerRepository.search(companyId, query, isCustomer, isVendor, includeArchived, pageable)
                .map(mapper::partnerToResponse);
        enrichWithImages(page.getContent());
        return page;
    }

    @Override
    @Transactional
    public PartnerResponse.AddressResponse addAddress(UUID partnerId, PartnerAddressCommand cmd) {
        Partner partner = loadOrThrow(partnerId);
        UUID addressId = UUID.randomUUID();
        PartnerAddress address = mapper.addressCommandToDomain(partner.getId(), addressId, cmd);
        partner.addAddress(address);
        Partner saved = partnerRepository.save(partner);
        auditLogPort.recordBusinessEvent(saved.getCompanyId(), MODEL_NAME, partnerId,
                "Address added: " + cmd.getType(), null);
        return mapper.addressToResponse(saved.findAddress(addressId));
    }

    @Override
    @Transactional
    public PartnerResponse.AddressResponse updateAddress(UUID partnerId, UUID addressId, PartnerAddressCommand cmd) {
        Partner partner = loadOrThrow(partnerId);
        PartnerAddress current = partner.findAddress(addressId);
        if (current.getType() != cmd.getType()) {
            throw new ContactsDomainException("Address type cannot be changed; remove + add");
        }
        current.update(cmd.getStreet1(), cmd.getStreet2(), cmd.getCity(),
                cmd.getState(), cmd.getPostalCode(), cmd.getCountry());
        if (cmd.isDefaultForType()) {
            partner.markAddressDefault(addressId);
        }
        Partner saved = partnerRepository.save(partner);
        return mapper.addressToResponse(saved.findAddress(addressId));
    }

    @Override
    @Transactional
    public void removeAddress(UUID partnerId, UUID addressId) {
        Partner partner = loadOrThrow(partnerId);
        partner.removeAddress(addressId);
        partnerRepository.save(partner);
    }

    @Override
    @Transactional
    public PartnerResponse.BankAccountResponse addBankAccount(UUID partnerId, PartnerBankAccountCommand cmd) {
        Partner partner = loadOrThrow(partnerId);
        UUID id = UUID.randomUUID();
        PartnerBankAccount account = mapper.bankAccountCommandToDomain(partner.getId(), id, cmd);
        partner.addBankAccount(account);
        Partner saved = partnerRepository.save(partner);
        return mapper.bankAccountToResponse(saved.getBankAccounts().stream()
                .filter(b -> b.getId().getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ContactsDomainException("Saved bank account missing")));
    }

    @Override
    @Transactional
    public void removeBankAccount(UUID partnerId, UUID bankAccountId) {
        Partner partner = loadOrThrow(partnerId);
        partner.removeBankAccount(bankAccountId);
        partnerRepository.save(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditStatusResponse creditStatus(UUID partnerId) {
        Partner partner = loadIncludingArchivedOrThrow(partnerId);
        if (!partner.isCustomer()) {
            throw new ContactsDomainException("Partner is not a customer");
        }
        PartnerBalancePort balancePort = partnerBalancePortProvider.getIfAvailable();
        Money outstanding = balancePort != null
                ? balancePort.outstandingReceivable(partner.getCompanyId(), partner.getId())
                : Money.ZERO;
        String companyCurrency = balancePort != null
                ? balancePort.companyBaseCurrencyCode(partner.getCompanyId())
                : "USD";
        CreditLimitChecker.CreditStatus status = CreditLimitChecker.check(partner, outstanding);
        return new CreditStatusResponse(
                partnerId,
                status.creditLimit().getAmount(),
                status.outstandingReceivable().getAmount(),
                status.available().getAmount(),
                status.unlimited(),
                companyCurrency);
    }

    private Partner loadOrThrow(UUID id) {
        return partnerRepository.findById(new PartnerId(id))
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + id));
    }

    private Partner loadIncludingArchivedOrThrow(UUID id) {
        return partnerRepository.findByIdIncludingArchived(new PartnerId(id))
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + id));
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

    private void publishPartnerUpdated(Partner partner) {
        contactsEventPublisher.publishPartnerUpdated(new PartnerUpdatedEvent(
                UUID.randomUUID(),
                java.time.Instant.now(),
                partner.getCompanyId().getId(),
                partner.getId().getId(),
                partner.isCustomer(),
                partner.isVendor()));
    }

    private PartnerResponse toResponse(Partner partner) {
        PartnerResponse response = mapper.partnerToResponse(partner);
        partnerRepository.findImageMeta(partner.getId().getId()).ifPresent(meta -> applyImage(response, meta));
        return response;
    }

    private void enrichWithImages(List<PartnerResponse> items) {
        if (items.isEmpty()) return;
        Map<UUID, PartnerImageMeta> meta = partnerRepository.findImageMetaByPartnerIds(
                items.stream().map(PartnerResponse::getId).collect(Collectors.toList()));
        for (PartnerResponse item : items) {
            PartnerImageMeta image = meta.get(item.getId());
            if (image != null) applyImage(item, image);
        }
    }

    private static void applyImage(PartnerResponse response, PartnerImageMeta meta) {
        response.setImageUrl(meta.imageUrl());
        response.setImageContentType(meta.contentType());
    }

    private void recordSystemActivity(CompanyId companyId, UUID recordId, String message) {
        CreateActivityCommand cmd = new CreateActivityCommand();
        cmd.setCompanyId(companyId.getId());
        cmd.setModelName(MODEL_NAME);
        cmd.setRecordId(recordId);
        cmd.setKind(ActivityKind.SYSTEM);
        cmd.setSubject(message);
        cmd.setBody(message);
        activityService.create(cmd);
    }
}
