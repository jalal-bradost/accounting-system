package com.jalaldeveloper.accountingsystem.platform.chatter.ports;

import com.jalaldeveloper.accountingsystem.platform.chatter.RecordAttachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordAttachmentRepository {

    List<RecordAttachment> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(
            UUID companyId, String modelName, UUID recordId);

    Optional<RecordAttachment> findById(UUID attachmentId);

    RecordAttachment save(RecordAttachment attachment);

    void delete(RecordAttachment attachment);
}
