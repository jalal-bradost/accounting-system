package com.jalaldeveloper.accountingsystem.sales.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalSalesOrderEntity;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SalSalesOrderJpaRepository extends JpaRepository<SalSalesOrderEntity, UUID> {

    Optional<SalSalesOrderEntity> findByCompanyIdAndName(UUID companyId, String name);

    @Query("select distinct o from SalSalesOrderEntity o left join fetch o.lines where o.id = :id")
    Optional<SalSalesOrderEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("""
            select o from SalSalesOrderEntity o
            where o.companyId = :companyId
            and (:state is null or o.state = :state)
            and (:customerId is null or o.customerPartnerId = :customerId)
            and (:q is null or lower(o.name) like lower(concat('%', :q, '%')))
            """)
    Page<SalSalesOrderEntity> search(@Param("companyId") UUID companyId,
                                      @Param("state") SalesOrderState state,
                                      @Param("customerId") UUID customerPartnerId,
                                      @Param("q") String q,
                                      Pageable pageable);
}
