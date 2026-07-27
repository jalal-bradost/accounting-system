package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayContractEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayContractJpaRepository extends JpaRepository<PayContractEntity, UUID> {
    Page<PayContractEntity> findByCompanyIdOrderByDateStartDesc(UUID companyId, Pageable pageable);
    Page<PayContractEntity> findByCompanyIdAndEmployeeIdOrderByDateStartDesc(UUID companyId, UUID employeeId, Pageable pageable);
    List<PayContractEntity> findByCompanyIdAndEmployeeIdAndStateOrderByDateStartDesc(UUID companyId, UUID employeeId, String state);
    Optional<PayContractEntity> findFirstByCompanyIdAndEmployeeIdAndStateOrderByDateStartDesc(UUID companyId, UUID employeeId, String state);
    long countByCompanyIdAndEmployeeTypeId(UUID companyId, UUID employeeTypeId);

    @Query("""
        SELECT c FROM PayContractEntity c
        WHERE c.companyId = :companyId
          AND c.state = 'running'
          AND c.dateStart <= :periodEnd
          AND (c.dateEnd IS NULL OR c.dateEnd >= :periodStart)
        ORDER BY c.dateStart DESC
        """)
    List<PayContractEntity> findRunningContractsInPeriod(@Param("companyId") UUID companyId,
                                                           @Param("periodStart") java.time.LocalDate periodStart,
                                                           @Param("periodEnd") java.time.LocalDate periodEnd);
}
