package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.SettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettingsJpaRepository extends JpaRepository<SettingsEntity, UUID> {

    Optional<SettingsEntity> findByCompanyId(UUID companyId);

    java.util.List<SettingsEntity> findByAutoLockAfterDaysNotNull();

    java.util.List<SettingsEntity> findByReminderEnabledTrue();

    java.util.List<SettingsEntity> findByLedgerPostingEnabledTrue();
}
