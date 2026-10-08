package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.SettingsRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/** Daily housekeeping. Idempotent: locking an already locked week changes nothing (TSH-05 #9). */
@Component
class TimesheetJobs {

    private final SettingsRepository settings;
    private final WeekRepository weeks;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final PostingRepository postings;
    private final PostingCoordinator coordinator;
    private final ReminderApplicationServiceImpl reminders;
    private final PostingApplicationServiceImpl postingService;

    TimesheetJobs(SettingsRepository settings, WeekRepository weeks, AuditLogPort audit, TimesheetAccess access,
                  PostingRepository postings, PostingCoordinator coordinator, ReminderApplicationServiceImpl reminders,
                  PostingApplicationServiceImpl postingService) {
        this.settings = settings;
        this.weeks = weeks;
        this.audit = audit;
        this.access = access;
        this.postings = postings;
        this.coordinator = coordinator;
        this.reminders = reminders;
        this.postingService = postingService;
    }

    /** TSH-10 #5: re-attempts failed postings (up to 5 times) and picks up postings stuck in PENDING. Idempotent. */
    @Scheduled(cron = "${app.timesheet.posting-retry-cron:0 20 * * * *}")
    public void retryPostings() {
        for (var p : postings.findRetryable(access.clock().instant().minusSeconds(300))) {
            coordinator.processNow(p.getId().getId());
        }
    }

    /** NFR accounting integrity: a daily comparison of approved cost and ledger postings. Reports, never changes anything. */
    @Scheduled(cron = "${app.timesheet.integrity-cron:0 30 3 * * *}")
    public void integrityCheck() {
        for (TimesheetSettings s : settings.findAllWithLedgerPosting()) {
            var report = postingService.check(s.getCompanyId());
            if (!report.ok()) {
                audit.recordBusinessEvent(s.getCompanyId(), "tsh.posting", s.getCompanyId().getId(), "Integrity check found problems",
                        Map.of("issues", report.issues().size(), "first", report.issues().get(0).kind() + ": " + report.issues().get(0).detail()));
            }
        }
    }

    /**
     * TSH-05 #8: every morning, companies whose reminder weekday is today (in their time zone) get their reminders.
     * Safe to run more than once a day: the reminder log allows one reminder per person per week.
     */
    @Scheduled(cron = "${app.timesheet.reminder-cron:0 0 7 * * *}")
    public void sendReminders() {
        for (TimesheetSettings s : settings.findAllWithReminders()) {
            if (access.today(s).getDayOfWeek() == s.getReminderWeekday()) {
                reminders.run(s.getCompanyId());
            }
        }
    }

    @Scheduled(cron = "${app.timesheet.auto-lock-cron:0 15 2 * * *}")
    @Transactional
    public void autoLock() {
        for (TimesheetSettings s : settings.findAllWithAutoLock()) {
            LocalDate cutoff = access.today(s).minusDays(s.getAutoLockAfterDays());
            int locked = weeks.lockStartedBefore(s.getCompanyId(), cutoff);
            if (locked > 0) {
                audit.recordBusinessEvent(s.getCompanyId(), "tsh.week", s.getCompanyId().getId(), "Weeks auto-locked",
                        Map.of("count", locked, "cutoff", cutoff.toString()));
            }
        }
    }
}
