package com.bradox.erp.sign.dataaccess.repository;

import com.bradox.erp.sign.dataaccess.entity.CounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CounterJpaRepository extends JpaRepository<CounterEntity, CounterEntity.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CounterEntity c where c.companyId = :companyId and c.counterYear = :year")
    Optional<CounterEntity> lockFor(@Param("companyId") UUID companyId, @Param("year") int year);
}
