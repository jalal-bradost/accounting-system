package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentRepository;
import com.bradox.erp.dataaccess.entity.AccCustomerPaymentEntity;
import com.bradox.erp.dataaccess.mapper.CustomerPaymentDataAccessMapper;
import com.bradox.erp.dataaccess.repository.AccCustomerPaymentJpaRepository;
import com.bradox.erp.domain.core.ValueObject.CustomerPaymentState;
import com.bradox.erp.domain.core.entity.CustomerPayment;
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
    public Optional<CustomerPayment> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::entityToDomain);
    }

    @Override
    public List<CustomerPayment> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByIdIn(ids).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CustomerPayment> searchByCompanyId(UUID companyId, Pageable pageable) {
        Page<UUID> idPage = jpaRepository.findIdsByCompanyId(companyId, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        Map<UUID, CustomerPayment> byId = jpaRepository.findByIdIn(idPage.getContent()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toMap(CustomerPayment::getId, Function.identity(), (a, b) -> a));
        List<CustomerPayment> ordered = new ArrayList<>(idPage.getContent().size());
        for (UUID id : idPage.getContent()) {
            CustomerPayment p = byId.get(id);
            if (p != null) {
                ordered.add(p);
            }
        }
        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
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

    @Override
    public List<CustomerPayment> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before) {
        return jpaRepository
                .findPostedByPartnerBefore(
                        companyId, partnerId, CustomerPaymentState.POSTED, before.atStartOfDay())
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPayment> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        return jpaRepository
                .findPostedByPartnerBetween(
                        companyId,
                        partnerId,
                        CustomerPaymentState.POSTED,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay())
                .stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPayment> findOpeningBalanceByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdAndOpeningBalanceTrueOrderByPaymentDateAscCreatedAtAsc(companyId).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }
}
