package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "journals", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
@Entity
public class JournalEntity {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalType type;
}