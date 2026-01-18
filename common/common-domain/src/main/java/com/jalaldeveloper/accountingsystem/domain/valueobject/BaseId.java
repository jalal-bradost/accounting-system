package com.jalaldeveloper.accountingsystem.domain.valueobject;

import java.util.Objects;

public abstract class BaseId<T> {
    private final T id;

    protected BaseId(final T id){
        this.id=id;
    }

    public T getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BaseId<?> baseId = (BaseId<?>) o;
        return Objects.equals(id, baseId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
