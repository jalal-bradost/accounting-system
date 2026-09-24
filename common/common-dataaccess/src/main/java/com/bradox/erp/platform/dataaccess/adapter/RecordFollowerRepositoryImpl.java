package com.bradox.erp.platform.dataaccess.adapter;

import com.bradox.erp.platform.chatter.RecordFollower;
import com.bradox.erp.platform.chatter.ports.RecordFollowerRepository;
import com.bradox.erp.platform.dataaccess.mapper.RecordFollowerDataAccessMapper;
import com.bradox.erp.platform.dataaccess.repository.RecordFollowerJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecordFollowerRepositoryImpl implements RecordFollowerRepository {

    private final RecordFollowerJpaRepository jpaRepository;
    private final RecordFollowerDataAccessMapper mapper;

    public RecordFollowerRepositoryImpl(RecordFollowerJpaRepository jpaRepository,
                                        RecordFollowerDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RecordFollower> findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtAsc(
            UUID companyId, String modelName, UUID recordId) {
        return jpaRepository.findByCompanyIdAndModelNameAndRecordIdOrderByCreatedAtAsc(companyId, modelName, recordId)
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Optional<RecordFollower> findByCompanyIdAndModelNameAndRecordIdAndPartnerId(
            UUID companyId, String modelName, UUID recordId, UUID partnerId) {
        return jpaRepository.findByCompanyIdAndModelNameAndRecordIdAndPartnerId(companyId, modelName, recordId, partnerId)
                .map(mapper::entityToDomain);
    }

    @Override
    public RecordFollower save(RecordFollower follower) {
        return mapper.entityToDomain(jpaRepository.save(mapper.domainToEntity(follower)));
    }

    @Override
    public void deleteById(UUID followerId) {
        jpaRepository.deleteById(followerId);
    }
}
