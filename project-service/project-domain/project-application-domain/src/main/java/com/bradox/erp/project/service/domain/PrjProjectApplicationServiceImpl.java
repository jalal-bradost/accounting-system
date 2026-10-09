package com.bradox.erp.project.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.activity.RecordActivityLogger;
import com.bradox.erp.project.domain.core.exception.ProjectDomainException;
import com.bradox.erp.project.domain.core.model.Project;
import com.bradox.erp.project.domain.core.model.Stage;
import com.bradox.erp.project.domain.core.model.Task;
import com.bradox.erp.project.service.domain.dto.MoveTaskCommand;
import com.bradox.erp.project.service.domain.dto.ProjectCommand;
import com.bradox.erp.project.service.domain.dto.ProjectResponse;
import com.bradox.erp.project.service.domain.dto.StageCommand;
import com.bradox.erp.project.service.domain.dto.StageResponse;
import com.bradox.erp.project.service.domain.dto.TaskCommand;
import com.bradox.erp.project.service.domain.dto.TaskResponse;
import com.bradox.erp.project.service.domain.ports.input.ProjectApplicationService;
import com.bradox.erp.project.service.domain.ports.output.ProjectRepository;
import com.bradox.erp.project.service.domain.ports.output.StageRepository;
import com.bradox.erp.project.service.domain.ports.output.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class PrjProjectApplicationServiceImpl implements ProjectApplicationService {

    private final ProjectRepository projects;
    private final StageRepository stages;
    private final TaskRepository tasks;
    private final ProjectAccess access;
    private final RecordActivityLogger chatter;

    PrjProjectApplicationServiceImpl(ProjectRepository projects, StageRepository stages, TaskRepository tasks, ProjectAccess access,
                                  RecordActivityLogger chatter) {
        this.projects = projects;
        this.stages = stages;
        this.tasks = tasks;
        this.access = access;
        this.chatter = chatter;
    }

    // ------------------------------------------------------------------ projects

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(CompanyId companyId) {
        access.require(ProjectPermissions.VIEW);
        Map<UUID, Long> counts = tasks.countByProject(companyId);
        return projects.findAll(companyId).stream().map(p -> toResponse(p, counts.getOrDefault(p.id(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(CompanyId companyId, UUID id) {
        access.require(ProjectPermissions.VIEW);
        return toResponse(loadProject(companyId, id), tasks.countByProject(companyId).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public ProjectResponse createProject(CompanyId companyId, ProjectCommand c) {
        access.require(ProjectPermissions.EDIT);
        Project p = projects.save(new Project(UUID.randomUUID(), companyId.getId(), c.name(), c.customerPartnerId(),
                c.managerUsername(), c.startDate(), c.endDate(), c.colorOrNone(), access.now()));
        int seq = 0;
        for (String name : Stage.DEFAULTS) {
            stages.save(new Stage(UUID.randomUUID(), companyId.getId(), p.id(), name, seq++));
        }
        return toResponse(p, 0);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(CompanyId companyId, UUID id, ProjectCommand c) {
        access.require(ProjectPermissions.EDIT);
        Project p = projects.save(loadProject(companyId, id).edit(c.name(), c.customerPartnerId(), c.managerUsername(),
                c.startDate(), c.endDate(), c.colorOrNone()));
        return toResponse(p, tasks.countByProject(companyId).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void deleteProject(CompanyId companyId, UUID id) {
        access.require(ProjectPermissions.EDIT);
        Project p = loadProject(companyId, id);
        tasks.deleteByProject(companyId, id);
        stages.deleteByProject(companyId, id);
        projects.delete(p);
    }

    // ------------------------------------------------------------------ stages

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> listStages(CompanyId companyId, UUID projectId) {
        access.require(ProjectPermissions.VIEW);
        loadProject(companyId, projectId);
        return stages.findByProject(companyId, projectId).stream().map(PrjProjectApplicationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public StageResponse createStage(CompanyId companyId, UUID projectId, StageCommand c) {
        access.require(ProjectPermissions.EDIT);
        loadProject(companyId, projectId);
        int next = stages.findByProject(companyId, projectId).stream().mapToInt(Stage::sequence).max().orElse(-1) + 1;
        return toResponse(stages.save(new Stage(UUID.randomUUID(), companyId.getId(), projectId, c.name(), next)));
    }

    @Override
    @Transactional
    public StageResponse renameStage(CompanyId companyId, UUID stageId, StageCommand c) {
        access.require(ProjectPermissions.EDIT);
        return toResponse(stages.save(loadStage(companyId, stageId).rename(c.name())));
    }

    @Override
    @Transactional
    public List<StageResponse> moveStage(CompanyId companyId, UUID stageId, int index) {
        access.require(ProjectPermissions.EDIT);
        Stage moved = loadStage(companyId, stageId);
        List<Stage> order = new ArrayList<>(stages.findByProject(companyId, moved.projectId()));
        order.removeIf(s -> s.id().equals(stageId));
        order.add(Math.max(0, Math.min(index, order.size())), moved);
        List<StageResponse> out = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            Stage s = order.get(i);
            out.add(toResponse(s.sequence() == i ? s : stages.save(s.at(i))));
        }
        return out;
    }

    @Override
    @Transactional
    public void deleteStage(CompanyId companyId, UUID stageId) {
        access.require(ProjectPermissions.EDIT);
        Stage s = loadStage(companyId, stageId);
        if (tasks.existsInStage(companyId, stageId)) {
            throw new ProjectDomainException("error.project.stage.notEmpty", null, "Move or delete the tasks of this stage first");
        }
        stages.delete(s);
    }

    // ------------------------------------------------------------------ tasks

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(CompanyId companyId, UUID projectId) {
        access.require(ProjectPermissions.VIEW);
        Project p = loadProject(companyId, projectId);
        Map<UUID, Stage> byId = stagesById(companyId, projectId);
        return tasks.findByProject(companyId, projectId).stream().map(t -> toResponse(t, p, byId.get(t.stageId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(CompanyId companyId, UUID id) {
        access.require(ProjectPermissions.VIEW);
        Task t = loadTask(companyId, id);
        return toResponse(t, loadProject(companyId, t.projectId()), stagesById(companyId, t.projectId()).get(t.stageId()));
    }

    @Override
    @Transactional
    public TaskResponse createTask(CompanyId companyId, UUID projectId, TaskCommand c) {
        access.require(ProjectPermissions.EDIT);
        Project p = loadProject(companyId, projectId);
        List<Stage> projectStages = stages.findByProject(companyId, projectId);
        Stage stage = c.stageId() == null
                ? projectStages.stream().findFirst().orElseThrow(() -> new ProjectDomainException("error.project.noStages", null,
                        "Add a stage to the project first"))
                : stageOf(projectStages, c.stageId());
        int next = tasks.findByProject(companyId, projectId).stream().filter(t -> t.stageId().equals(stage.id()))
                .mapToInt(Task::sequence).max().orElse(-1) + 1;
        Task t = tasks.save(new Task(UUID.randomUUID(), companyId.getId(), projectId, stage.id(), c.name(), c.description(),
                c.customerPartnerId() != null ? c.customerPartnerId() : p.customerPartnerId(), c.assigneeUsername(), c.deadline(),
                c.priorityOrZero(), next, access.now()));
        chatter.log(companyId.getId(), RecordActivityLogger.MODEL_PROJECT_TASK, t.id(), "Task created in " + stage.name());
        return toResponse(t, p, stage);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(CompanyId companyId, UUID id, TaskCommand c) {
        access.require(ProjectPermissions.EDIT);
        Task before = loadTask(companyId, id);
        List<Stage> projectStages = stages.findByProject(companyId, before.projectId());
        Stage stage = stageOf(projectStages, c.stageId() == null ? before.stageId() : c.stageId());
        Task t = before.edit(stage.id(), c.name(), c.description(), c.customerPartnerId(), c.assigneeUsername(), c.deadline(),
                c.priorityOrZero());
        if (!stage.id().equals(before.stageId())) {
            t = t.moveTo(stage.id(), nextSequence(companyId, before.projectId(), stage.id()));
        }
        Task saved = tasks.save(t);
        logStageChange(companyId, before, stage, projectStages);
        return toResponse(saved, loadProject(companyId, saved.projectId()), stage);
    }

    @Override
    @Transactional
    public TaskResponse moveTask(CompanyId companyId, UUID id, MoveTaskCommand c) {
        access.require(ProjectPermissions.EDIT);
        Task moved = loadTask(companyId, id);
        List<Stage> projectStages = stages.findByProject(companyId, moved.projectId());
        Stage target = stageOf(projectStages, c.stageId());
        List<Task> column = new ArrayList<>(tasks.findByProject(companyId, moved.projectId()).stream()
                .filter(t -> t.stageId().equals(target.id()) && !t.id().equals(id)).toList());
        column.add(Math.max(0, Math.min(c.index(), column.size())), moved);
        Task result = moved;
        for (int i = 0; i < column.size(); i++) {
            Task t = column.get(i);
            if (t.id().equals(id)) {
                result = tasks.save(t.moveTo(target.id(), i));
            } else if (t.sequence() != i) {
                tasks.save(t.moveTo(target.id(), i));
            }
        }
        logStageChange(companyId, moved, target, projectStages);
        return toResponse(result, loadProject(companyId, moved.projectId()), target);
    }

    @Override
    @Transactional
    public void deleteTask(CompanyId companyId, UUID id) {
        access.require(ProjectPermissions.EDIT);
        tasks.delete(loadTask(companyId, id));
    }

    // ------------------------------------------------------------------ helpers

    private void logStageChange(CompanyId companyId, Task before, Stage target, List<Stage> projectStages) {
        if (target.id().equals(before.stageId())) {
            return;
        }
        String from = projectStages.stream().filter(s -> s.id().equals(before.stageId())).map(Stage::name).findFirst().orElse("?");
        chatter.log(companyId.getId(), RecordActivityLogger.MODEL_PROJECT_TASK, before.id(),
                "Stage: " + from + " → " + target.name());
    }

    private int nextSequence(CompanyId companyId, UUID projectId, UUID stageId) {
        return tasks.findByProject(companyId, projectId).stream().filter(t -> t.stageId().equals(stageId))
                .mapToInt(Task::sequence).max().orElse(-1) + 1;
    }

    private Map<UUID, Stage> stagesById(CompanyId companyId, UUID projectId) {
        return stages.findByProject(companyId, projectId).stream().collect(Collectors.toMap(Stage::id, Function.identity()));
    }

    private static Stage stageOf(List<Stage> projectStages, UUID stageId) {
        return projectStages.stream().filter(s -> s.id().equals(stageId)).findFirst()
                .orElseThrow(() -> new ProjectDomainException("error.project.stage.other", null, "The stage belongs to another project"));
    }

    private Project loadProject(CompanyId companyId, UUID id) {
        return projects.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Stage loadStage(CompanyId companyId, UUID id) {
        return stages.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found"));
    }

    private Task loadTask(CompanyId companyId, UUID id) {
        return tasks.find(companyId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private static ProjectResponse toResponse(Project p, long taskCount) {
        return new ProjectResponse(p.id(), p.name(), p.customerPartnerId(), p.managerUsername(), p.startDate(), p.endDate(),
                p.color(), taskCount, p.createdAt());
    }

    private static StageResponse toResponse(Stage s) {
        return new StageResponse(s.id(), s.projectId(), s.name(), s.sequence());
    }

    private static TaskResponse toResponse(Task t, Project p, Stage s) {
        return new TaskResponse(t.id(), t.projectId(), p.name(), t.stageId(), s == null ? null : s.name(), t.name(),
                t.description(), t.customerPartnerId(), t.assigneeUsername(), t.deadline(), t.priority(), t.sequence(),
                t.createdAt());
    }
}
