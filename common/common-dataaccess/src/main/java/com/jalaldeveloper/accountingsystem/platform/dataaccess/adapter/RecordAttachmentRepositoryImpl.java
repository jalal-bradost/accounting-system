package com.jalaldeveloper.accountingsystem.platform.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.platform.chatter.RecordAttachment;
import com.jalaldeveloper.accountingsystem.platform.chatter.ports.RecordAttachmentRepository;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.RecordAttachmentEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.mapper.RecordAttachmentDataAccessMapper;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.RecordAttachmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecordAttachmentRepositoryImpl implements RecordAttachmentRepository {

    private final RecordAttachmentJpaRepository jpaRepository;
    private final RecordAttachmentDataAccessMapper mapper;

    public RecordAttachmentRepositoryImpl(RecordAttachmentJpaRepository jpaRepository,
                                            RecordAttachmentDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RecordAttachment> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId) {
        return jpaRepository.findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(companyId, modelName, recordId)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Optional<RecordAttachment> findById(UUID attachmentId) {
        return jpaRepository.findById(attachmentId).map(mapper::entityToDomain);
    }

    @Override
    public RecordAttachment save(RecordAttachment attachment) {
        return mapper.entityToDomain(jpaRepository.save(mapper.domainToEntity(attachment)));
    }

    @Override
    public void delete(RecordAttachment attachment) {
        RecordAttachmentEntity entity = jpaRepository.findById(attachment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachment.getId()));
        jpaRepository.delete(entity);
    }
}
