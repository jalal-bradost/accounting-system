package com.bradox.erp.platform.web;

import com.bradox.erp.platform.dataaccess.entity.AppUserEntity;
import com.bradox.erp.platform.dataaccess.repository.AppUserJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves app-user ids to a human-readable display label (display name, else username).
 * Also accepts already-resolved labels or legacy stored UUID strings.
 */
@Service
public class UserDisplayNameService {

    private final AppUserJpaRepository users;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public UserDisplayNameService(AppUserJpaRepository users) {
        this.users = users;
    }

    public String forUserId(UUID userId) {
        if (userId == null) {
            return "system";
        }
        return cache.computeIfAbsent(userId, id -> users.findById(id)
                .map(UserDisplayNameService::labelOf)
                .orElse(id.toString()));
    }

    /**
     * If {@code stored} looks like a user UUID, resolve to the display label; otherwise return as-is.
     */
    public String resolve(String stored) {
        if (stored == null || stored.isBlank() || "system".equals(stored)) {
            return stored;
        }
        Optional<UUID> id = parseUuid(stored);
        if (id.isEmpty()) {
            return stored;
        }
        return forUserId(id.get());
    }

    public void putCached(UUID userId, String display) {
        if (userId != null && display != null && !display.isBlank()) {
            cache.put(userId, display);
        }
    }

    public static String labelOf(AppUserEntity user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return user.getId() != null ? user.getId().toString() : "system";
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
