package com.bradox.erp.accounting.service.domain.ports.output.repository;

import com.bradox.erp.domain.core.entity.CustomerPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPaymentRepository {

    CustomerPayment save(CustomerPayment payment);

    Optional<CustomerPayment> findById(UUID id);

    /** Loads the payment under a pessimistic write lock (held until the transaction ends). */
    Optional<CustomerPayment> findByIdForUpdate(UUID id);

    List<CustomerPayment> findByIdIn(Collection<UUID> ids);

    List<CustomerPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId);

    Page<CustomerPayment> searchByCompanyId(UUID companyId, Pageable pageable);

    List<CustomerPayment> findByCompanyIdAndCustomerPartnerIdOrderByPaymentDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);

    List<CustomerPayment> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before);

    List<CustomerPayment> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to);

    List<CustomerPayment> findOpeningBalanceByCompanyId(UUID companyId);
}
