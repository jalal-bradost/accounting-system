package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.bradox.erp.dataaccess.entity.AccCustomerInvoiceEntity;
import com.bradox.erp.dataaccess.mapper.CustomerInvoiceDataAccessMapper;
import com.bradox.erp.dataaccess.repository.AccCustomerInvoiceJpaRepository;
import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;
import com.bradox.erp.domain.core.entity.CustomerInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomerInvoiceRepositoryImpl implements CustomerInvoiceRepository {

    private final AccCustomerInvoiceJpaRepository jpaRepository;
    private final CustomerInvoiceDataAccessMapper mapper;

    public CustomerInvoiceRepositoryImpl(AccCustomerInvoiceJpaRepository jpaRepository,
                                         CustomerInvoiceDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CustomerInvoice save(CustomerInvoice invoice) {
        AccCustomerInvoiceEntity existing = jpaRepository.findById(invoice.getId()).orElse(null);
        AccCustomerInvoiceEntity toSave = mapper.domainToEntity(invoice, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<CustomerInvoice> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    @Override
    public Optional<CustomerInvoice> findByIdWithLines(UUID id) {
        return jpaRepository.findByIdWithLines(id).map(mapper::entityToDomain);
    }

    @Override
    public List<CustomerInvoice> findByCompanyWithLines(UUID companyId) {
        return jpaRepository.findByCompanyWithLines(companyId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CustomerInvoice> search(UUID companyId, CustomerInvoiceState state, UUID salesOrderId, String q,
                                        Pageable pageable) {
        String qNorm = q != null ? q.trim() : "";
        boolean qBlank = qNorm.isEmpty();
        Page<UUID> idPage = jpaRepository.findIdsByCompany(
                companyId, state, salesOrderId, qNorm, qBlank, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        Map<UUID, CustomerInvoice> byId = jpaRepository.findByIdInWithLines(idPage.getContent()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toMap(CustomerInvoice::getId, Function.identity(), (a, b) -> a));
        List<CustomerInvoice> ordered = new ArrayList<>(idPage.getContent().size());
        for (UUID id : idPage.getContent()) {
            CustomerInvoice inv = byId.get(id);
            if (inv != null) {
                ordered.add(inv);
            }
        }
        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Override
    public boolean existsBySalesOrderIdAndState(UUID salesOrderId, CustomerInvoiceState state) {
        return jpaRepository.existsBySalesOrderIdAndState(salesOrderId, state);
    }

    @Override
    public List<CustomerInvoice> findBySalesOrderIdWithLines(UUID salesOrderId) {
        return jpaRepository.findBySalesOrderIdWithLines(salesOrderId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findBySalesOrderIdInWithLines(Collection<UUID> salesOrderIds) {
        if (salesOrderIds == null || salesOrderIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findBySalesOrderIdInWithLines(salesOrderIds).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId) {
        return jpaRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(companyId, customerPartnerId)
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before) {
        return jpaRepository
                .findPostedByPartnerBefore(companyId, partnerId, CustomerInvoiceState.POSTED, before)
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        return jpaRepository
                .findPostedByPartnerBetween(companyId, partnerId, CustomerInvoiceState.POSTED, from, to)
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findByReversedInvoiceIdWithLines(UUID reversedInvoiceId) {
        return jpaRepository.findByReversedInvoiceIdWithLines(reversedInvoiceId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findByReversedInvoiceIdIn(java.util.Collection<UUID> reversedInvoiceIds) {
        if (reversedInvoiceIds == null || reversedInvoiceIds.isEmpty()) return List.of();
        return jpaRepository.findByReversedInvoiceIdIn(reversedInvoiceIds).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerInvoice> findByIdIn(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpaRepository.findByIdIn(ids).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void lockById(UUID id) {
        jpaRepository.lockById(id);
    }

    @Override
    public List<CustomerInvoice> findOpeningBalanceByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdAndOpeningBalanceTrueOrderByInvoiceDateAscCreatedAtAsc(companyId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }
}
