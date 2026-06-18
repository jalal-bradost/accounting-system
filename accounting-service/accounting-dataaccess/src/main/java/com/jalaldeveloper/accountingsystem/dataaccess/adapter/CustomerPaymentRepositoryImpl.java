package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.mapper.CustomerPaymentDataAccessMapper;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccCustomerPaymentJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerPayment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CustomerPaymentRepositoryImpl implements CustomerPaymentRepository {

    private final AccCustomerPaymentJpaRepository jpaRepository;
    private final CustomerPaymentDataAccessMapper mapper;

    public CustomerPaymentRepositoryImpl(AccCustomerPaymentJpaRepository jpaRepository,
                                         CustomerPaymentDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CustomerPayment save(CustomerPayment payment) {
        return mapper.entityToDomain(jpaRepository.save(mapper.domainToEntity(payment)));
    }

    @Override
    public List<CustomerPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPayment> findByCustomerInvoiceId(UUID customerInvoiceId) {
        return jpaRepository.findByCustomerInvoiceId(customerInvoiceId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPayment> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId) {
        return jpaRepository
                .findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, customerPartnerId)
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }
}
