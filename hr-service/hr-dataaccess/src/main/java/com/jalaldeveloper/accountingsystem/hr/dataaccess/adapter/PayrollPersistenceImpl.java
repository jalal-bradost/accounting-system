package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.*;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.*;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.payroll.PayrollPersistence;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class PayrollPersistenceImpl implements PayrollPersistence {

    private final PayWorkingScheduleJpaRepository scheduleRepository;
    private final PayEmployeeTypeJpaRepository employeeTypeRepository;
    private final PayStructureTypeJpaRepository structureTypeRepository;
    private final PayStructureJpaRepository structureRepository;
    private final PayContractJpaRepository contractRepository;
    private final EmployeeJpaRepository employeeRepository;
    private final AttendanceJpaRepository attendanceJpaRepository;

    public PayrollPersistenceImpl(PayWorkingScheduleJpaRepository scheduleRepository,
                                  PayEmployeeTypeJpaRepository employeeTypeRepository,
                                  PayStructureTypeJpaRepository structureTypeRepository,
                                  PayStructureJpaRepository structureRepository,
                                  PayContractJpaRepository contractRepository,
                                  EmployeeJpaRepository employeeRepository,
                                  AttendanceJpaRepository attendanceJpaRepository) {
        this.scheduleRepository = scheduleRepository;
        this.employeeTypeRepository = employeeTypeRepository;
        this.structureTypeRepository = structureTypeRepository;
        this.structureRepository = structureRepository;
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceJpaRepository = attendanceJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleRow> listSchedules(UUID companyId) {
        return scheduleRepository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
                .map(this::toScheduleRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScheduleRow> findSchedule(UUID id) {
        return scheduleRepository.findById(id).map(this::toScheduleRow);
    }

    @Override
    @Transactional
    public ScheduleRow saveSchedule(ScheduleRow row) {
        PayWorkingScheduleEntity entity = row.id() != null
                ? scheduleRepository.findById(row.id()).orElseGet(PayWorkingScheduleEntity::new)
                : new PayWorkingScheduleEntity();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCompanyId(row.companyId());
        entity.setName(row.name());
        entity.setTwoWeekCalendar(row.twoWeekCalendar());
        entity.setSortOrder(row.sortOrder());
        entity.getLines().clear();
        int i = 0;
        for (ScheduleLineRow line : row.lines()) {
            PayWorkingScheduleLineEntity le = new PayWorkingScheduleLineEntity();
            le.setId(line.id() != null ? line.id() : UUID.randomUUID());
            le.setSchedule(entity);
            le.setDayOfWeek(line.dayOfWeek());
            le.setHours(line.hours());
            le.setSortOrder(line.sortOrder() >= 0 ? line.sortOrder() : i++);
            entity.getLines().add(le);
        }
        return toScheduleRow(scheduleRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeTypeRow> listEmployeeTypes(UUID companyId) {
        return employeeTypeRepository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
                .map(this::toEmployeeTypeRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeTypeRow> findEmployeeType(UUID id) {
        return employeeTypeRepository.findById(id).map(this::toEmployeeTypeRow);
    }

    @Override
    @Transactional
    public EmployeeTypeRow saveEmployeeType(EmployeeTypeRow row) {
        PayEmployeeTypeEntity entity = row.id() != null
                ? employeeTypeRepository.findById(row.id()).orElseGet(PayEmployeeTypeEntity::new)
                : new PayEmployeeTypeEntity();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCompanyId(row.companyId());
        entity.setName(row.name());
        entity.setCountryCode(row.countryCode());
        entity.setSortOrder(row.sortOrder());
        return toEmployeeTypeRow(employeeTypeRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countContractsByEmployeeType(UUID companyId, UUID employeeTypeId) {
        return contractRepository.countByCompanyIdAndEmployeeTypeId(companyId, employeeTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructureTypeRow> listStructureTypes(UUID companyId) {
        return structureTypeRepository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
                .map(this::toStructureTypeRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructureTypeRow> findStructureType(UUID id) {
        return structureTypeRepository.findById(id).map(this::toStructureTypeRow);
    }

    @Override
    @Transactional
    public StructureTypeRow saveStructureType(StructureTypeRow row) {
        PayStructureTypeEntity entity = row.id() != null
                ? structureTypeRepository.findById(row.id()).orElseGet(PayStructureTypeEntity::new)
                : new PayStructureTypeEntity();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCompanyId(row.companyId());
        entity.setName(row.name());
        entity.setScheduledPay(row.scheduledPay());
        entity.setWageType(row.wageType());
        entity.setWorkingScheduleId(row.workingScheduleId());
        entity.setCountryCode(row.countryCode());
        entity.setPayStructureId(row.payStructureId());
        entity.setSortOrder(row.sortOrder());
        return toStructureTypeRow(structureTypeRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructureRow> listStructures(UUID companyId) {
        return structureRepository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
                .map(this::toStructureRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructureRow> findStructure(UUID id) {
        return structureRepository.findById(id).map(this::toStructureRow);
    }

    @Override
    @Transactional
    public StructureRow saveStructure(StructureRow row) {
        PayStructureEntity entity = row.id() != null
                ? structureRepository.findById(row.id()).orElseGet(PayStructureEntity::new)
                : new PayStructureEntity();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCompanyId(row.companyId());
        entity.setName(row.name());
        entity.setStructureTypeId(row.structureTypeId());
        entity.setScheduledPay(row.scheduledPay());
        entity.setUseWorkedDayLines(row.useWorkedDayLines());
        entity.setCountryCode(row.countryCode());
        entity.setSortOrder(row.sortOrder());
        entity.getRules().clear();
        for (SalaryRuleRow rule : row.rules()) {
            PaySalaryRuleEntity re = new PaySalaryRuleEntity();
            re.setId(rule.id() != null ? rule.id() : UUID.randomUUID());
            re.setStructure(entity);
            re.setName(rule.name());
            re.setCode(rule.code());
            re.setCategory(rule.category());
            re.setAmountType(rule.amountType());
            re.setAmount(rule.amount());
            re.setSequence(rule.sequence());
            re.setActive(rule.active());
            re.setAccountId(rule.accountId());
            entity.getRules().add(re);
        }
        return toStructureRow(structureRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRow> listContracts(UUID companyId, UUID employeeId, int page, int size) {
        var result = employeeId != null
                ? contractRepository.findByCompanyIdAndEmployeeIdOrderByDateStartDesc(
                        companyId, employeeId, PageRequest.of(page, size))
                : contractRepository.findByCompanyIdOrderByDateStartDesc(
                        companyId, PageRequest.of(page, size));
        return result.getContent().stream().map(this::toContractRow).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countContracts(UUID companyId, UUID employeeId) {
        if (employeeId != null) {
            return contractRepository.findByCompanyIdAndEmployeeIdOrderByDateStartDesc(
                    companyId, employeeId, PageRequest.of(0, 1)).getTotalElements();
        }
        return contractRepository.findByCompanyIdOrderByDateStartDesc(
                companyId, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractRow> findContract(UUID id) {
        return contractRepository.findById(id).map(this::toContractRow);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractRow> findRunningContract(UUID companyId, UUID employeeId) {
        return contractRepository.findFirstByCompanyIdAndEmployeeIdAndStateOrderByDateStartDesc(
                companyId, employeeId, "running").map(this::toContractRow);
    }

    @Override
    @Transactional
    public ContractRow saveContract(ContractRow row) {
        PayContractEntity entity = row.id() != null
                ? contractRepository.findById(row.id()).orElseGet(PayContractEntity::new)
                : new PayContractEntity();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setCompanyId(row.companyId());
        entity.setEmployeeId(row.employeeId());
        entity.setName(row.name());
        entity.setEmployeeTypeId(row.employeeTypeId());
        entity.setStructureId(row.structureId());
        entity.setWorkingScheduleId(row.workingScheduleId());
        entity.setWage(row.wage());
        entity.setWageType(row.wageType());
        entity.setCurrencyCode(row.currencyCode());
        entity.setDateStart(row.dateStart());
        entity.setDateEnd(row.dateEnd());
        entity.setState(row.state());
        entity.setAttendanceBased(row.attendanceBased());
        return toContractRow(contractRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findEmployeeName(UUID employeeId) {
        if (employeeId == null) return Optional.empty();
        return employeeRepository.findById(employeeId).map(EmployeeEntity::getDisplayName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findScheduleName(UUID scheduleId) {
        if (scheduleId == null) return Optional.empty();
        return scheduleRepository.findById(scheduleId).map(PayWorkingScheduleEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findStructureName(UUID structureId) {
        if (structureId == null) return Optional.empty();
        return structureRepository.findById(structureId).map(PayStructureEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findEmployeeTypeName(UUID employeeTypeId) {
        if (employeeTypeId == null) return Optional.empty();
        return employeeTypeRepository.findById(employeeTypeId).map(PayEmployeeTypeEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findStructureTypeName(UUID structureTypeId) {
        if (structureTypeId == null) return Optional.empty();
        return structureTypeRepository.findById(structureTypeId).map(PayStructureTypeEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean employeeExists(UUID employeeId) {
        return employeeRepository.existsById(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRow> listRunningContracts(UUID companyId, LocalDate periodStart, LocalDate periodEnd) {
        return contractRepository.findRunningContractsInPeriod(companyId, periodStart, periodEnd).stream()
                .map(this::toContractRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal countWorkedDays(UUID employeeId, LocalDate start, LocalDate end) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(zone).toInstant();
        return BigDecimal.valueOf(attendanceJpaRepository.countWorkedDays(employeeId, from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal countExpectedWorkDays(UUID scheduleId, LocalDate start, LocalDate end) {
        PayWorkingScheduleEntity schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getLines().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Short> workDays = new HashSet<>();
        for (PayWorkingScheduleLineEntity line : schedule.getLines()) {
            if (line.getHours().signum() > 0) {
                workDays.add(line.getDayOfWeek());
            }
        }
        if (workDays.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            short dow = (short) d.getDayOfWeek().getValue();
            if (workDays.contains(dow)) {
                count++;
            }
        }
        return BigDecimal.valueOf(count);
    }

    private ScheduleRow toScheduleRow(PayWorkingScheduleEntity e) {
        List<ScheduleLineRow> lines = e.getLines().stream()
                .map(l -> new ScheduleLineRow(l.getId(), l.getDayOfWeek(), l.getHours(), l.getSortOrder()))
                .toList();
        return new ScheduleRow(e.getId(), e.getCompanyId(), e.getName(), e.isTwoWeekCalendar(), e.getSortOrder(), lines);
    }

    private EmployeeTypeRow toEmployeeTypeRow(PayEmployeeTypeEntity e) {
        return new EmployeeTypeRow(e.getId(), e.getCompanyId(), e.getName(), e.getCountryCode(), e.getSortOrder());
    }

    private StructureTypeRow toStructureTypeRow(PayStructureTypeEntity e) {
        return new StructureTypeRow(e.getId(), e.getCompanyId(), e.getName(), e.getScheduledPay(), e.getWageType(),
                e.getWorkingScheduleId(), e.getCountryCode(), e.getPayStructureId(), e.getSortOrder());
    }

    private StructureRow toStructureRow(PayStructureEntity e) {
        List<SalaryRuleRow> rules = e.getRules().stream()
                .map(r -> new SalaryRuleRow(r.getId(), r.getName(), r.getCode(), r.getCategory(), r.getAmountType(),
                        r.getAmount(), r.getSequence(), r.isActive(), r.getAccountId()))
                .toList();
        return new StructureRow(e.getId(), e.getCompanyId(), e.getName(), e.getStructureTypeId(), e.getScheduledPay(),
                e.isUseWorkedDayLines(), e.getCountryCode(), e.getSortOrder(), rules);
    }

    private ContractRow toContractRow(PayContractEntity e) {
        return new ContractRow(e.getId(), e.getCompanyId(), e.getEmployeeId(), e.getName(), e.getEmployeeTypeId(),
                e.getStructureId(), e.getWorkingScheduleId(), e.getWage(), e.getWageType(), e.getCurrencyCode(),
                e.getDateStart(), e.getDateEnd(), e.getState(), e.isAttendanceBased());
    }

    static BigDecimal totalHours(List<ScheduleLineRow> lines) {
        return lines.stream().map(ScheduleLineRow::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
