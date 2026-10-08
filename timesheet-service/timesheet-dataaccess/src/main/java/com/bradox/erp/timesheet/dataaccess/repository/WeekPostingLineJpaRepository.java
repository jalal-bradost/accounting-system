package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.WeekPostingLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WeekPostingLineJpaRepository extends JpaRepository<WeekPostingLineEntity, UUID> {

    List<WeekPostingLineEntity> findByPostingIdIn(Collection<UUID> postingIds);

    void deleteByPostingId(UUID postingId);
}
