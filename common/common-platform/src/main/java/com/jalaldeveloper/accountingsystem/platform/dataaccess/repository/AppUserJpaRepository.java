package com.jalaldeveloper.accountingsystem.platform.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AppUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByCompanyIdAndUsername(UUID companyId, String username);

    Optional<AppUserEntity> findByCompanyIdAndEmail(UUID companyId, String email);

    @Query("select u from AppUserEntity u where u.companyId = :companyId "
            + "and (:q is null or :q = '' "
            + "  or lower(u.username) like lower(concat('%', :q, '%')) "
            + "  or lower(u.email) like lower(concat('%', :q, '%')) "
            + "  or lower(coalesce(u.displayName, '')) like lower(concat('%', :q, '%'))) "
            + "and (:active is null or u.active = :active)")
    Page<AppUserEntity> search(@Param("companyId") UUID companyId,
                               @Param("q") String q,
                               @Param("active") Boolean active,
                               Pageable pageable);
}
