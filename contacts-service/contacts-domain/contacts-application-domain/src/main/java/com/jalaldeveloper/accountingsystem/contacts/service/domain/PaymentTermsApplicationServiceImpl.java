package com.jalaldeveloper.accountingsystem.contacts.service.domain;

import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PaymentTerms;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PaymentTermsCommand;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.dto.PaymentTermsResponse;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.mapper.ContactsDataMapper;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.input.PaymentTermsApplicationService;
import com.jalaldeveloper.accountingsystem.contacts.service.domain.ports.output.repository.PaymentTermsRepository;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
class PaymentTermsApplicationServiceImpl implements PaymentTermsApplicationService {

    private final PaymentTermsRepository repository;
    private final ContactsDataMapper mapper;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    PaymentTermsApplicationServiceImpl(PaymentTermsRepository repository,
                                       ContactsDataMapper mapper,
                                       ObjectProvider<CompanyContext> companyContextProvider) {
        this.repository = repository;
        this.mapper = mapper;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public PaymentTermsResponse create(PaymentTermsCommand cmd) {
        CompanyId companyId = resolveCompany(cmd.getCompanyId());
        if (repository.existsByCompanyIdAndName(companyId, cmd.getName())) {
            throw new IllegalArgumentException("Payment terms already exist with name: " + cmd.getName());
        }
        PaymentTerms terms = mapper.paymentTermsCommandToDomain(UUID.randomUUID(), companyId, cmd);
        terms.validate();
        return mapper.paymentTermsToResponse(repository.save(terms));
    }

    @Override
    @Transactional
    public PaymentTermsResponse update(UUID id, PaymentTermsCommand cmd) {
        PaymentTerms existing = loadOrThrow(id);
        PaymentTerms updated = PaymentTerms.builder()
                .id(existing.getId())
                .companyId(existing.getCompanyId())
                .name(cmd.getName() != null ? cmd.getName() : existing.getName())
                .daysNet(cmd.getDaysNet())
                .discountDays(cmd.getDiscountDays())
                .discountPercent(cmd.getDiscountPercent() != null ? cmd.getDiscountPercent() : existing.getDiscountPercent())
                .build();
        if (existing.isActive() != updated.isActive()) {
            throw new ContactsDomainException("Use archive/unarchive to change active flag");
        }
        updated.validate();
        return mapper.paymentTermsToResponse(repository.save(updated));
    }

    @Override
    @Transactional
    public PaymentTermsResponse archive(UUID id) {
        PaymentTerms terms = loadOrThrow(id);
        terms.archive(currentUserDisplay());
        return mapper.paymentTermsToResponse(repository.save(terms));
    }

    @Override
    @Transactional
    public PaymentTermsResponse unarchive(UUID id) {
        PaymentTerms terms = loadOrThrow(id);
        terms.unarchive();
        return mapper.paymentTermsToResponse(repository.save(terms));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTermsResponse get(UUID id) {
        return mapper.paymentTermsToResponse(loadOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTermsResponse> list(CompanyId companyId, boolean includeArchived) {
        return repository.findByCompanyId(companyId, includeArchived).stream()
                .map(mapper::paymentTermsToResponse)
                .toList();
    }

    private PaymentTerms loadOrThrow(UUID id) {
        return repository.findById(new PaymentTermsId(id))
                .orElseThrow(() -> new IllegalArgumentException("Payment terms not found: " + id));
    }

    private CompanyId resolveCompany(UUID explicit) {
        if (explicit != null) return new CompanyId(explicit);
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany().orElseThrow(() ->
                    new IllegalArgumentException("companyId required"));
        }
        throw new IllegalArgumentException("companyId required");
    }

    private String currentUserDisplay() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }
}
