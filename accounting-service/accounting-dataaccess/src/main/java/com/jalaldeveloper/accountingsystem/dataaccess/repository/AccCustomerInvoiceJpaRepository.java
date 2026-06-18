package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccCustomerInvoiceJpaRepository extends JpaRepository<AccCustomerInvoiceEntity, UUID> {

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.companyId = :companyId order by i.invoiceDate desc, i.createdAt desc")
    List<AccCustomerInvoiceEntity> findByCompanyWithLines(@Param("companyId") UUID companyId);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.id = :id")
    Optional<AccCustomerInvoiceEntity> findByIdWithLines(@Param("id") UUID id);

    boolean existsBySalesOrderIdAndState(UUID salesOrderId, CustomerInvoiceState state);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.salesOrderId = :salesOrderId")
    List<AccCustomerInvoiceEntity> findBySalesOrderIdWithLines(@Param("salesOrderId") UUID salesOrderId);

    List<AccCustomerInvoiceEntity> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);
}
