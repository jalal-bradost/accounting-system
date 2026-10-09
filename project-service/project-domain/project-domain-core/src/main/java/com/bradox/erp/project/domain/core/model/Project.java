package com.bradox.erp.project.domain.core.model;

import com.bradox.erp.project.domain.core.exception.ProjectDomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A project: a name, optionally a customer, a manager and planned dates, and a colour for its card. Its tasks move
 * through the project's own stages.
 */
public record Project(UUID id, UUID companyId, String name, UUID customerPartnerId, String managerUsername,
                      LocalDate startDate, LocalDate endDate, int color, Instant createdAt) {

    /** Number of colours the screens offer; 0 means none. */
    public static final int COLORS = 12;

    public Project {
        if (name == null || name.isBlank()) {
            throw new ProjectDomainException("error.project.name", null, "A project needs a name");
        }
        name = name.trim();
        managerUsername = managerUsername == null || managerUsername.isBlank() ? null : managerUsername.trim();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ProjectDomainException("error.project.dates", null, "The end date cannot be before the start date");
        }
        if (color < 0 || color >= COLORS) {
            color = 0;
        }
    }

    public Project edit(String newName, UUID customer, String manager, LocalDate start, LocalDate end, int newColor) {
        return new Project(id, companyId, newName, customer, manager, start, end, newColor, createdAt);
    }
}
