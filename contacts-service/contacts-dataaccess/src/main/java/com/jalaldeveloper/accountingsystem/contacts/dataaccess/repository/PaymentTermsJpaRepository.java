package com.jalaldeveloper.accountingsystem.contacts.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PaymentTermsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTermsJpaRepository extends JpaRepository<PaymentTermsEntity, UUID> {

    List<PaymentTermsEntity> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);

    List<PaymentTermsEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
