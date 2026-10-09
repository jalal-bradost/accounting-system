package com.bradox.erp.project.domain.core.model;

import com.bradox.erp.project.domain.core.exception.ProjectDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectModelTest {

    private static final UUID COMPANY = UUID.randomUUID();

    @Test
    void projectNeedsANameAndOrderedDates() {
        assertThatThrownBy(() -> new Project(UUID.randomUUID(), COMPANY, " ", null, null, null, null, 0, Instant.now()))
                .isInstanceOf(ProjectDomainException.class);
        assertThatThrownBy(() -> new Project(UUID.randomUUID(), COMPANY, "X", null, null, LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1), 0, Instant.now())).isInstanceOf(ProjectDomainException.class);
        Project p = new Project(UUID.randomUUID(), COMPANY, " Office ", null, " ", null, null, 99, Instant.now());
        assertThat(p.name()).isEqualTo("Office");
        assertThat(p.managerUsername()).isNull();
        assertThat(p.color()).isZero();
    }

    @Test
    void taskNeedsATitleAStageAndAValidPriority() {
        UUID stage = UUID.randomUUID();
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), COMPANY, UUID.randomUUID(), stage, "", null, null, null, null, 0, 0,
                Instant.now())).isInstanceOf(ProjectDomainException.class);
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), COMPANY, UUID.randomUUID(), null, "T", null, null, null, null, 0, 0,
                Instant.now())).isInstanceOf(ProjectDomainException.class);
        assertThatThrownBy(() -> new Task(UUID.randomUUID(), COMPANY, UUID.randomUUID(), stage, "T", null, null, null, null, 4, 0,
                Instant.now())).isInstanceOf(ProjectDomainException.class);
    }
}
