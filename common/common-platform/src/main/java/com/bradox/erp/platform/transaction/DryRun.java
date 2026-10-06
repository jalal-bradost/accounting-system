package com.bradox.erp.platform.transaction;

import java.util.function.Supplier;

/**
 * Marks the current thread as running a dry run: business logic executes for real inside a
 * transaction that is rolled back afterwards (exact previews). Side channels that write in their
 * own transaction, such as the audit log, check {@link #isActive()} and skip writing.
 */
public final class DryRun {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DryRun() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static <T> T run(Supplier<T> action) {
        boolean outer = ACTIVE.get();
        ACTIVE.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            ACTIVE.set(outer);
        }
    }
}
