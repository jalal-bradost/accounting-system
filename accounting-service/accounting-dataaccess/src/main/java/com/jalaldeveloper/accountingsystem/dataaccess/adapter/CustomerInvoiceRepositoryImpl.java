package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerInvoiceRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.mapper.CustomerInvoiceDataAccessMapper;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccCustomerInvoiceJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    public List<CustomerInvoice> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId) {
        return jpaRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(companyId, customerPartnerId)
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }
}
