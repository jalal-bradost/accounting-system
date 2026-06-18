package com.jalaldeveloper.accountingsystem.platform.chatter;

import com.jalaldeveloper.accountingsystem.platform.chatter.ports.RecordAttachmentRepository;
import com.jalaldeveloper.accountingsystem.platform.chatter.ports.RecordFollowerRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class ChatterApplicationServiceImpl implements ChatterApplicationService {

    private final RecordFollowerRepository followerRepository;
    private final RecordAttachmentRepository attachmentRepository;
    private final ChatterAttachmentStorage storage;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ChatterApplicationServiceImpl(RecordFollowerRepository followerRepository,
                                  RecordAttachmentRepository attachmentRepository,
                                  ChatterAttachmentStorage storage,
                                  ObjectProvider<CompanyContext> companyContextProvider) {
        this.followerRepository = followerRepository;
        this.attachmentRepository = attachmentRepository;
        this.storage = storage;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatterFollowerResponse> listFollowers(UUID companyId, String modelName, UUID recordId) {
        return followerRepository
                .findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtAsc(companyId, modelName, recordId)
                .stream()
                .map(this::toFollowerResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ChatterFollowerResponse> addFollowers(AddFollowersCommand command) {
        UUID companyId = command.getCompanyId() != null ? command.getCompanyId() : currentCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("companyId required");
        }
        String user = currentUser();
        Instant now = Instant.now();
        List<ChatterFollowerResponse> created = new ArrayList<>();

        for (UUID partnerId : command.getPartnerIds()) {
            if (partnerId == null) continue;
            if (followerRepository.findByCompanyIdAndModelNameAndRecordIdAndPartnerId(
                    companyId, command.getModelName(), command.getRecordId(), partnerId).isPresent()) {
                continue;
            }
            RecordFollower follower = new RecordFollower();
            follower.setId(UUID.randomUUID());
            follower.setCompanyId(companyId);
            follower.setModelName(command.getModelName());
            follower.setRecordId(command.getRecordId());
            follower.setPartnerId(partnerId);
            follower.setAddedBy(user);
            follower.setNotifyOnPost(command.isNotifyRecipients());
            follower.setCreatedAt(now);
            created.add(toFollowerResponse(followerRepository.save(follower)));
        }
        return created;
    }

    @Override
    @Transactional
    public void removeFollower(UUID followerId) {
        followerRepository.deleteById(followerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatterAttachmentResponse> listAttachments(UUID companyId, String modelName, UUID recordId) {
        return attachmentRepository
                .findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtDesc(companyId, modelName, recordId)
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatterAttachmentResponse uploadAttachment(UUID companyId, String modelName, UUID recordId,
                                                      MultipartFile file) {
        UUID resolvedCompanyId = companyId != null ? companyId : currentCompanyId();
        if (resolvedCompanyId == null) {
            throw new IllegalArgumentException("companyId required");
        }
        ChatterAttachmentStorage.StoredFile stored =
                storage.store(resolvedCompanyId, modelName, recordId, file);

        RecordAttachment attachment = new RecordAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setCompanyId(resolvedCompanyId);
        attachment.setModelName(modelName);
        attachment.setRecordId(recordId);
        attachment.setFileName(stored.fileName());
        attachment.setContentType(stored.contentType());
        attachment.setFileSize(stored.fileSize());
        attachment.setPublicUrl(stored.publicUrl());
        attachment.setUploadedBy(currentUser());
        attachment.setCreatedAt(Instant.now());
        return toAttachmentResponse(attachmentRepository.save(attachment));
    }

    @Override
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        RecordAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        storage.deleteByPublicUrl(attachment.getPublicUrl());
        attachmentRepository.delete(attachment);
    }

    private ChatterFollowerResponse toFollowerResponse(RecordFollower e) {
        return new ChatterFollowerResponse(
                e.getId(), e.getCompanyId(), e.getModelName(), e.getRecordId(),
                e.getPartnerId(), e.getAddedBy(), e.isNotifyOnPost(), e.getCreatedAt());
    }

    private ChatterAttachmentResponse toAttachmentResponse(RecordAttachment e) {
        return new ChatterAttachmentResponse(
                e.getId(), e.getCompanyId(), e.getModelName(), e.getRecordId(),
                e.getFileName(), e.getContentType(), e.getFileSize(),
                e.getPublicUrl(), e.getUploadedBy(), e.getCreatedAt());
    }

    private UUID currentCompanyId() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? null : ctx.currentCompany().map(c -> c.getId()).orElse(null);
    }

    private String currentUser() {
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        return ctx == null ? "system" : ctx.currentUserDisplay();
    }
}
