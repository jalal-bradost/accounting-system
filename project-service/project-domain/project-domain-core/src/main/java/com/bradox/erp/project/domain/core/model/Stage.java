package com.bradox.erp.project.domain.core.model;

import com.bradox.erp.project.domain.core.exception.ProjectDomainException;

import java.util.List;
import java.util.UUID;

/** A column of a project's task board, such as New, In Progress or Done. */
public record Stage(UUID id, UUID companyId, UUID projectId, String name, int sequence) {

    /** The stages every new project starts with. */
    public static final List<String> DEFAULTS = List.of("New", "In Progress", "Done");

    public Stage {
        if (name == null || name.isBlank()) {
            throw new ProjectDomainException("error.project.stage.name", null, "A stage needs a name");
        }
        name = name.trim();
    }

    public Stage rename(String newName) {
        return new Stage(id, companyId, projectId, newName, sequence);
    }

    public Stage at(int newSequence) {
        return new Stage(id, companyId, projectId, name, newSequence);
    }
}
