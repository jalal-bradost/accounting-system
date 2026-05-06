package com.jalaldeveloper.accountingsystem.platform.audit;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic JPA entity listener that captures CREATE / UPDATE / DELETE diffs for
 * any entity annotated with {@link AuditableModel} into the audit log. Per-field
 * tracking is opt-in via {@link AuditTrack}; non-annotated fields are ignored to
 * avoid noisy bookkeeping columns.
 *
 * <p>Attach with {@code @EntityListeners(AuditingEntityListener.class)} on the
 * concrete entity class.
 *
 * <p>Best-effort: never throws inside lifecycle callbacks. Failures are logged.
 */
public class AuditingEntityListener {

    private static final Logger log = LoggerFactory.getLogger(AuditingEntityListener.class);

    @PostPersist
    public void onCreate(Object entity) {
        write(entity, AuditAction.CREATE);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        write(entity, AuditAction.UPDATE);
    }

    @PostRemove
    public void onDelete(Object entity) {
        write(entity, AuditAction.DELETE);
    }

    private void write(Object entity, AuditAction action) {
        try {
            AuditLogPort port = AuditContextHolder.auditLogPort();
            if (port == null) return;

            AuditableModel model = entity.getClass().getAnnotation(AuditableModel.class);
            if (model == null) return;

            Map<String, Object> snapshot = snapshot(entity);
            UUID recordId = readId(entity);
            CompanyId companyId = readCompanyId(entity);

            port.recordCrud(companyId, action, model.value(), recordId, snapshot);
        } catch (Throwable ex) {
            log.warn("Audit listener failed on {}: {}", entity.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private static Map<String, Object> snapshot(Object entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                AuditTrack track = field.getAnnotation(AuditTrack.class);
                if (track == null) continue;
                String name = (track.name().isEmpty() ? field.getName() : track.name());
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    out.put(name, value != null ? value.toString() : null);
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return out;
    }

    private static UUID readId(Object entity) {
        return readField(entity, "id", UUID.class);
    }

    private static CompanyId readCompanyId(Object entity) {
        UUID raw = readField(entity, "companyId", UUID.class);
        if (raw != null) return new CompanyId(raw);
        CompanyContext ctx = AuditContextHolder.currentContext();
        if (ctx == null) return null;
        try {
            return ctx.currentCompany().orElse(null);
        } catch (RuntimeException ex) {
            // No active request scope; skip context-based fallback.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object entity, String name, Class<T> type) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                Object v = field.get(entity);
                return type.isInstance(v) ? (T) v : null;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }
}
