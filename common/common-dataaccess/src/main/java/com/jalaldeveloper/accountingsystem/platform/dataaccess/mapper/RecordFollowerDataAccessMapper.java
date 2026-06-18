package com.jalaldeveloper.accountingsystem.platform.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.platform.chatter.RecordFollower;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RecordFollowerEntity;
import org.springframework.stereotype.Component;

@Component
public class RecordFollowerDataAccessMapper {

    public RecordFollower entityToDomain(RecordFollowerEntity entity) {
        if (entity == null) return null;
        RecordFollower domain = new RecordFollower();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setModelName(entity.getModelName());
        domain.setRecordId(entity.getRecordId());
        domain.setPartnerId(entity.getPartnerId());
        domain.setAddedBy(entity.getAddedBy());
        domain.setNotifyOnPost(entity.isNotifyOnPost());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public RecordFollowerEntity domainToEntity(RecordFollower domain) {
        if (domain == null) return null;
        RecordFollowerEntity entity = new RecordFollowerEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setModelName(domain.getModelName());
        entity.setRecordId(domain.getRecordId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setAddedBy(domain.getAddedBy());
        entity.setNotifyOnPost(domain.isNotifyOnPost());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
