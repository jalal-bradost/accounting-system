package com.jalaldeveloper.accountingsystem.sales.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalPricelistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalPricelistJpaRepository extends JpaRepository<SalPricelistEntity, UUID> {

    @Query("select distinct p from SalPricelistEntity p left join fetch p.items where p.id = :id")
    Optional<SalPricelistEntity> findByIdWithItems(@Param("id") UUID id);

    List<SalPricelistEntity> findByCompanyIdAndActive(UUID companyId, boolean active);
}
