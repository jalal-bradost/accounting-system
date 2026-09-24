package com.bradox.erp.dataaccess.repository;

import com.bradox.erp.dataaccess.entity.CompanySettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanySettingsJpaRepository extends JpaRepository<CompanySettingsEntity, UUID> {}