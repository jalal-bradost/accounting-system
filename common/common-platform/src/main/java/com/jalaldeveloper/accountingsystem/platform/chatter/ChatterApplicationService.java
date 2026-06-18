package com.jalaldeveloper.accountingsystem.platform.chatter;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ChatterApplicationService {

    List<ChatterFollowerResponse> listFollowers(UUID companyId, String modelName, UUID recordId);

    List<ChatterFollowerResponse> addFollowers(AddFollowersCommand command);

    void removeFollower(UUID followerId);

    List<ChatterAttachmentResponse> listAttachments(UUID companyId, String modelName, UUID recordId);

    ChatterAttachmentResponse uploadAttachment(UUID companyId, String modelName, UUID recordId, MultipartFile file);

    void deleteAttachment(UUID attachmentId);
}
