package com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.UomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UomJpaRepository extends JpaRepository<UomEntity, UUID> {

    @Query("""
        SELECT u FROM UomEntity u
        WHERE u.categoryId = :categoryId
          AND (:includeArchived = TRUE OR u.active = TRUE)
        ORDER BY u.factor ASC, u.name ASC
        """)
    List<UomEntity> findByCategory(@Param("categoryId") UUID categoryId,
                                    @Param("includeArchived") boolean includeArchived);

    @Query("""
        SELECT u FROM UomEntity u
        WHERE u.companyId = :companyId
          AND (:includeArchived = TRUE OR u.active = TRUE)
        ORDER BY u.name ASC
        """)
    List<UomEntity> findByCompany(@Param("companyId") UUID companyId,
                                   @Param("includeArchived") boolean includeArchived);
}
