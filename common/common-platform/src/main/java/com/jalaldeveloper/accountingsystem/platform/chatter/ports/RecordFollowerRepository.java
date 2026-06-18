package com.jalaldeveloper.accountingsystem.platform.chatter.ports;

import com.jalaldeveloper.accountingsystem.platform.chatter.RecordFollower;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordFollowerRepository {

    List<RecordFollower> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtAsc(
            UUID companyId, String modelName, UUID recordId);

    Optional<RecordFollower> findByCompanyIdAndModelNameAndRecordIdAndPartnerId(
            UUID companyId, String modelName, UUID recordId, UUID partnerId);

    RecordFollower save(RecordFollower follower);

    void deleteById(UUID followerId);
}
