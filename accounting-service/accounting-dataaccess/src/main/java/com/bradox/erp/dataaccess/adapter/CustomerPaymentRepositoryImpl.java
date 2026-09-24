package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.bradox.erp.dataaccess.entity.AccCustomerPaymentEntity;
import com.bradox.erp.dataaccess.mapper.CustomerPaymentDataAccessMapper;
import com.bradox.erp.dataaccess.repository.AccCustomerPaymentJpaRepository;
import com.bradox.erp.domain.core.entity.CustomerPayment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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
        AccCustomerPaymentEntity entity = mapper.domainToEntity(payment);
        return mapper.entityToDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<CustomerPayment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::entityToDomain);
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
