package com.jalaldeveloper.accountingsystem.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

@Component
public class JournalDataAccessMapper {

    public Journal entityToDomain(JournalEntity entity) {
        if (entity == null) return null;
        return Journal.builder()
                .id(new JournalId(entity.getId()))
                .companyId(new CompanyId(entity.getCompanyId()))
                .code(entity.getCode())
                .name(entity.getName())
                .journalType(entity.getType())
                .build();
    }

    public JournalEntity domainToEntity(Journal domain) {
        if (domain == null) return null;
        return JournalEntity.builder()
                .id(domain.getId().getId())
                .companyId(domain.getCompanyId().getId())
                .code(domain.getCode())
                .name(domain.getName())
                .type(domain.getJournalType())
                .build();
    }
}
