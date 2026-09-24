package com.bradox.erp.purchase.dataaccess.repository;

import com.bradox.erp.purchase.dataaccess.entity.PurFiscalTaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurFiscalTaxJpaRepository extends JpaRepository<PurFiscalTaxEntity, UUID> {

    List<PurFiscalTaxEntity> findByCompanyIdAndActive(UUID companyId, boolean active);
}
