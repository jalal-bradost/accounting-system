package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.AccCustomerInvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bradox.erp.domain.core.ValueObject.CustomerInvoiceState;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccCustomerInvoiceJpaRepository extends JpaRepository<AccCustomerInvoiceEntity, UUID> {

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.companyId = :companyId order by i.invoiceDate desc, i.createdAt desc")
    List<AccCustomerInvoiceEntity> findByCompanyWithLines(@Param("companyId") UUID companyId);

    @Query(
            value = """
                    select i.id from AccCustomerInvoiceEntity i
                    where i.companyId = :companyId
                      and (:state is null or i.state = :state)
                      and (:salesOrderId is null or i.salesOrderId = :salesOrderId)
                      and (:qBlank = true or lower(coalesce(i.reference, '')) like lower(concat('%', :q, '%')))
                    order by i.invoiceDate desc, i.createdAt desc
                    """,
            countQuery = """
                    select count(i) from AccCustomerInvoiceEntity i
                    where i.companyId = :companyId
                      and (:state is null or i.state = :state)
                      and (:salesOrderId is null or i.salesOrderId = :salesOrderId)
                      and (:qBlank = true or lower(coalesce(i.reference, '')) like lower(concat('%', :q, '%')))
                    """)
    Page<UUID> findIdsByCompany(
            @Param("companyId") UUID companyId,
            @Param("state") CustomerInvoiceState state,
            @Param("salesOrderId") UUID salesOrderId,
            @Param("q") String q,
            @Param("qBlank") boolean qBlank,
            Pageable pageable);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.id in :ids")
    List<AccCustomerInvoiceEntity> findByIdInWithLines(@Param("ids") Collection<UUID> ids);

    @Query("""
            select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l
            where i.companyId = :companyId and i.customerPartnerId = :partnerId
              and i.state = :state and i.invoiceDate < :before
            order by i.invoiceDate asc, i.createdAt asc
            """)
    List<AccCustomerInvoiceEntity> findPostedByPartnerBefore(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") CustomerInvoiceState state,
            @Param("before") LocalDate before);

    @Query("""
            select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l
            where i.companyId = :companyId and i.customerPartnerId = :partnerId
              and i.state = :state and i.invoiceDate >= :from and i.invoiceDate <= :to
            order by i.invoiceDate asc, i.createdAt asc
            """)
    List<AccCustomerInvoiceEntity> findPostedByPartnerBetween(
            @Param("companyId") UUID companyId,
            @Param("partnerId") UUID partnerId,
            @Param("state") CustomerInvoiceState state,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.id = :id")
    Optional<AccCustomerInvoiceEntity> findByIdWithLines(@Param("id") UUID id);

    boolean existsBySalesOrderIdAndState(UUID salesOrderId, CustomerInvoiceState state);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.salesOrderId = :salesOrderId")
    List<AccCustomerInvoiceEntity> findBySalesOrderIdWithLines(@Param("salesOrderId") UUID salesOrderId);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.salesOrderId in :salesOrderIds")
    List<AccCustomerInvoiceEntity> findBySalesOrderIdInWithLines(@Param("salesOrderIds") Collection<UUID> salesOrderIds);

    List<AccCustomerInvoiceEntity> findByCompanyIdAndCustomerPartnerIdOrderByInvoiceDateAscCreatedAtAsc(
            UUID companyId, UUID customerPartnerId);

    @Query("select distinct i from AccCustomerInvoiceEntity i left join fetch i.lines l where i.reversedInvoiceId = :reversedInvoiceId order by i.invoiceDate desc, i.createdAt desc")
    List<AccCustomerInvoiceEntity> findByReversedInvoiceIdWithLines(@Param("reversedInvoiceId") UUID reversedInvoiceId);

    List<AccCustomerInvoiceEntity> findByReversedInvoiceIdIn(java.util.Collection<UUID> reversedInvoiceIds);

    List<AccCustomerInvoiceEntity> findByIdIn(java.util.Collection<UUID> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from AccCustomerInvoiceEntity i where i.id = :id")
    Optional<AccCustomerInvoiceEntity> lockById(@Param("id") UUID id);

    List<AccCustomerInvoiceEntity> findByCompanyIdAndOpeningBalanceTrueOrderByInvoiceDateAscCreatedAtAsc(UUID companyId);
}
