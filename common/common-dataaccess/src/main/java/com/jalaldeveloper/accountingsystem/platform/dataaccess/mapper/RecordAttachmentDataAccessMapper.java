package com.jalaldeveloper.accountingsystem.platform.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.platform.chatter.RecordAttachment;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RecordAttachmentEntity;
import org.springframework.stereotype.Component;

@Component
public class RecordAttachmentDataAccessMapper {

    public RecordAttachment entityToDomain(RecordAttachmentEntity entity) {
        if (entity == null) return null;
        RecordAttachment domain = new RecordAttachment();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setModelName(entity.getModelName());
        domain.setRecordId(entity.getRecordId());
        domain.setFileName(entity.getFileName());
        domain.setContentType(entity.getContentType());
        domain.setFileSize(entity.getFileSize());
        domain.setPublicUrl(entity.getPublicUrl());
        domain.setUploadedBy(entity.getUploadedBy());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public RecordAttachmentEntity domainToEntity(RecordAttachment domain) {
        if (domain == null) return null;
        RecordAttachmentEntity entity = new RecordAttachmentEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setModelName(domain.getModelName());
        entity.setRecordId(domain.getRecordId());
        entity.setFileName(domain.getFileName());
        entity.setContentType(domain.getContentType());
        entity.setFileSize(domain.getFileSize());
        entity.setPublicUrl(domain.getPublicUrl());
        entity.setUploadedBy(domain.getUploadedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
