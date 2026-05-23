package com.jalaldeveloper.accountingsystem.pos.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PosOrderJpaRepository extends JpaRepository<PosOrderEntity, UUID> {
    @Query("""
            select distinct o from PosOrderEntity o
            left join fetch o.lines
            left join fetch o.payments
            where o.id = :id
            """)
    Optional<PosOrderEntity> findByIdWithLinesAndPayments(@Param("id") UUID id);

    long countByCompanyId(UUID companyId);
}
