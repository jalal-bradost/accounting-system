package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.CompanyCurrencyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyCurrencyJpaRepository extends JpaRepository<CompanyCurrencyEntity, UUID> {

    boolean existsByCompanyId(UUID companyId);

    @Query(
            "select c from CompanyCurrencyEntity c where c.companyId = :companyId "
                    + "and (:q is null or :q = '' "
                    + "  or lower(c.code) like lower(concat('%', :q, '%')) "
                    + "  or lower(c.name) like lower(concat('%', :q, '%')) "
                    + "  or lower(c.symbol) like lower(concat('%', :q, '%')))")
    Page<CompanyCurrencyEntity> search(
            @Param("companyId") UUID companyId, @Param("q") String q, Pageable pageable);

    Optional<CompanyCurrencyEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<CompanyCurrencyEntity> findByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    List<CompanyCurrencyEntity> findByCompanyIdOrderByBaseCurrencyDescCodeAsc(UUID companyId);
}
