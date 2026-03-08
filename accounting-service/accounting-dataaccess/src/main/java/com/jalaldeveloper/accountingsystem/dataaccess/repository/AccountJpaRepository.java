package com.jalaldeveloper.accountingsystem.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByCompanyIdAndCode(UUID companyId, String code);

    List<AccountEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
