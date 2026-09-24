package com.bradox.erp.domain.valueobject;

import java.util.UUID;

public class CompanyId extends BaseId<UUID> {
    public CompanyId(UUID value){
        super(value);
    }
}
