package com.bradox.delin.platform.dataaccess.repository;

import com.bradox.delin.platform.dataaccess.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, UUID> {

    List<CompanyEntity> findAllByOrderByNameAsc();

    List<CompanyEntity> findAllByIdInOrderByNameAsc(List<UUID> ids);
}
