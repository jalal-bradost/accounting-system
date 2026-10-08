package com.bradox.erp.timesheet.dataaccess.repository;

import com.bradox.erp.timesheet.dataaccess.entity.EntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EntryJpaRepository extends JpaRepository<EntryEntity, UUID>, JpaSpecificationExecutor<EntryEntity> {

    List<EntryEntity> findByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(
            UUID companyId, UUID employeeId, LocalDate from, LocalDate to);

    List<EntryEntity> findByCompanyIdAndEmployeeIdAndWorkDateAndProjectIdAndTaskId(
            UUID companyId, UUID employeeId, LocalDate workDate, UUID projectId, UUID taskId);

    List<EntryEntity> findByCompanyIdAndEmployeeIdAndWorkDateAndProjectIdAndTaskIdIsNull(
            UUID companyId, UUID employeeId, LocalDate workDate, UUID projectId);

    boolean existsByProjectId(UUID projectId);

    List<EntryEntity> findByWeekId(UUID weekId);

    @Query("select e.saleLineId, sum(e.minutes) from EntryEntity e where e.companyId = :companyId and e.billable = true "
            + "and e.saleLineId in :ids and e.weekId in (select w.id from WeekEntity w where w.status = 'APPROVED') "
            + "group by e.saleLineId")
    List<Object[]> sumApprovedBillableByLine(@Param("companyId") UUID companyId, @Param("ids") Collection<UUID> ids);

    @Query("select e from EntryEntity e where e.companyId = :companyId and e.billable = true and e.saleLineId in :ids "
            + "and e.weekId in (select w.id from WeekEntity w where w.status = 'APPROVED') order by e.workDate, e.createdAt")
    List<EntryEntity> findApprovedBillableByLines(@Param("companyId") UUID companyId, @Param("ids") Collection<UUID> ids);

    @Query("select e from EntryEntity e where e.companyId = :companyId and e.billable = true and e.saleLineId is null "
            + "and e.weekId in (select w.id from WeekEntity w where w.status = 'APPROVED') "
            + "and e.projectId in (select p.id from ProjectEntity p where p.billingMode = 'HOURLY') order by e.workDate")
    List<EntryEntity> findApprovedBillableWithoutLine(@Param("companyId") UUID companyId);

    List<EntryEntity> findByCompanyIdAndRecordModelAndRecordIdOrderByWorkDateDesc(UUID companyId, String recordModel, UUID recordId);

    List<EntryEntity> findByCompanyIdAndCostMissingTrue(UUID companyId);

    @Query("select e.weekId, sum(e.minutes) from EntryEntity e where e.weekId in :ids group by e.weekId")
    List<Object[]> minutesByWeek(@Param("ids") Collection<UUID> ids);

    @Query("select coalesce(sum(e.minutes), 0), coalesce(sum(case when e.billable = true then e.minutes else 0 end), 0), "
            + "coalesce(sum(e.costAmount), 0), coalesce(sum(case when e.costMissing = true then 1 else 0 end), 0) "
            + "from EntryEntity e where e.companyId = :companyId and e.projectId = :projectId")
    List<Object[]> projectTotals(@Param("companyId") UUID companyId, @Param("projectId") UUID projectId);

    /** {@code excludeId} is a sentinel UUID when nothing is excluded, which keeps the parameter typed on PostgreSQL. */
    @Query("select coalesce(sum(e.minutes), 0) from EntryEntity e where e.companyId = :companyId "
            + "and e.employeeId = :employeeId and e.workDate = :date and e.id <> :excludeId")
    long minutesOnDay(@Param("companyId") UUID companyId, @Param("employeeId") UUID employeeId,
                      @Param("date") LocalDate date, @Param("excludeId") UUID excludeId);

    @Query("select e.projectId, sum(e.minutes) from EntryEntity e where e.companyId = :companyId "
            + "and e.projectId in :ids group by e.projectId")
    List<Object[]> minutesByProject(@Param("companyId") UUID companyId, @Param("ids") Collection<UUID> ids);

    @Query("select e.taskId, sum(e.minutes) from EntryEntity e where e.companyId = :companyId "
            + "and e.taskId in :ids group by e.taskId")
    List<Object[]> minutesByTask(@Param("companyId") UUID companyId, @Param("ids") Collection<UUID> ids);
}
