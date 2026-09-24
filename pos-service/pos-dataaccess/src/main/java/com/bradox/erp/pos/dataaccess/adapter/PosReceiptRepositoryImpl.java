package com.bradox.erp.pos.dataaccess.adapter;

import com.bradox.erp.pos.dataaccess.mapper.PosDataAccessMapper;
import com.bradox.erp.pos.dataaccess.repository.PosReceiptJpaRepository;
import com.bradox.erp.pos.domain.core.entity.PosReceipt;
import com.bradox.erp.pos.service.domain.ports.output.repository.PosReceiptRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PosReceiptRepositoryImpl implements PosReceiptRepository {

    private final PosReceiptJpaRepository jpa;
    private final PosDataAccessMapper mapper;

    public PosReceiptRepositoryImpl(PosReceiptJpaRepository jpa, PosDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PosReceipt save(PosReceipt receipt) {
        var existing = jpa.findById(receipt.getId()).orElse(null);
        return mapper.entityToDomain(jpa.save(mapper.domainToEntity(receipt, existing)));
    }

    @Override
    public Optional<PosReceipt> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public long countByCompanyId(UUID companyId) {
        return jpa.countByCompanyId(companyId);
    }
}
