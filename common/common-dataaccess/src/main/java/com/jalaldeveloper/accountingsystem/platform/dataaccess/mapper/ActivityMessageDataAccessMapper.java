package com.jalaldeveloper.accountingsystem.platform.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.platform.activity.ActivityMessage;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ActivityMessageEntity;
import org.springframework.stereotype.Component;

@Component
public class ActivityMessageDataAccessMapper {

    public ActivityMessage entityToDomain(ActivityMessageEntity entity) {
        if (entity == null) return null;
        ActivityMessage domain = new ActivityMessage();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setModelName(entity.getModelName());
        domain.setRecordId(entity.getRecordId());
        domain.setKind(entity.getKind());
        domain.setSubject(entity.getSubject());
        domain.setBody(entity.getBody());
        domain.setAuthorId(entity.getAuthorId());
        domain.setAssigneeId(entity.getAssigneeId());
        domain.setDueDate(entity.getDueDate());
        domain.setCompletedAt(entity.getCompletedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public ActivityMessageEntity domainToEntity(ActivityMessage domain) {
        if (domain == null) return null;
        ActivityMessageEntity entity = new ActivityMessageEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setModelName(domain.getModelName());
        entity.setRecordId(domain.getRecordId());
        entity.setKind(domain.getKind());
        entity.setSubject(domain.getSubject());
        entity.setBody(domain.getBody());
        entity.setAuthorId(domain.getAuthorId());
        entity.setAssigneeId(domain.getAssigneeId());
        entity.setDueDate(domain.getDueDate());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
