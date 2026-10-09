-- Project: projects, the stages of their task boards, and tasks. Standalone: partners and users are referenced by id
-- and username only.

CREATE TABLE IF NOT EXISTS prj_project(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    customer_partner_id UUID,
    manager_username VARCHAR(255),
    start_date DATE,
    end_date DATE,
    color INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_prj_project_company ON prj_project(company_id);

CREATE TABLE IF NOT EXISTS prj_stage(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    sequence INTEGER NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_prj_stage_project ON prj_stage(company_id, project_id);

CREATE TABLE IF NOT EXISTS prj_task(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    project_id UUID NOT NULL,
    stage_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    customer_partner_id UUID,
    assignee_username VARCHAR(255),
    deadline DATE,
    priority INTEGER NOT NULL,
    sequence INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_prj_task_project ON prj_task(company_id, project_id);
CREATE INDEX ix_prj_task_stage ON prj_task(company_id, stage_id);
