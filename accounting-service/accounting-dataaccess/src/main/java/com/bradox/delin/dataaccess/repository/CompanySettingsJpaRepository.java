package com.bradox.delin.dataaccess.repository;

import com.bradox.delin.dataaccess.entity.CompanySettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanySettingsJpaRepository extends JpaRepository<CompanySettingsEntity, UUID> {}