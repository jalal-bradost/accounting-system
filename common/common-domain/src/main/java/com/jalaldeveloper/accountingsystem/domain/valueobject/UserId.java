package com.jalaldeveloper.accountingsystem.domain.valueobject;

import java.util.UUID;

public class UserId extends BaseId<UUID> {
    public UserId(UUID value) {
        super(value);
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }
}
