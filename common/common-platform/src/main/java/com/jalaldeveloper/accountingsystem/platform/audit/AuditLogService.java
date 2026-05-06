package com.jalaldeveloper.accountingsystem.platform.audit;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.AuditLogEntity;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.repository.AuditLogJpaRepository;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AuditLogService implements AuditLogPort {

    private final AuditLogJpaRepository repository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    public AuditLogService(AuditLogJpaRepository repository,
                           ObjectProvider<CompanyContext> companyContextProvider) {
        this.repository = repository;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBusinessEvent(CompanyId companyId,
                                    String modelName,
                                    UUID recordId,
                                    String message,
                                    Map<String, Object> changes) {
        write(AuditAction.BUSINESS_EVENT, companyId, modelName, recordId, message, changes);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCrud(CompanyId companyId,
                           AuditAction action,
                           String modelName,
                           UUID recordId,
                           Map<String, Object> changes) {
        write(action, companyId, modelName, recordId, null, changes);
    }

    private void write(AuditAction action,
                       CompanyId companyId,
                       String modelName,
                       UUID recordId,
                       String message,
                       Map<String, Object> changes) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId != null ? companyId.getId() : currentCompanyIdOrNull());
        entity.setUserId(currentUserOrSystem());
        entity.setAction(action);
        entity.setModelName(modelName);
        entity.setRecordId(recordId);
        entity.setMessage(message);
        entity.setChangesJson(toCompactJson(changes));
        entity.setOccurredAt(Instant.now());
        repository.save(entity);
    }

    private UUID currentCompanyIdOrNull() {
        try {
            CompanyContext ctx = companyContextProvider.getIfAvailable();
            return ctx == null ? null : ctx.currentCompany().map(c -> c.getId()).orElse(null);
        } catch (RuntimeException ex) {
            // No active request scope (e.g. seeders, async background tasks).
            return null;
        }
    }

    private String currentUserOrSystem() {
        try {
            CompanyContext ctx = companyContextProvider.getIfAvailable();
            return ctx == null ? "system" : ctx.currentUserDisplay();
        } catch (RuntimeException ex) {
            return "system";
        }
    }

    /** Compact, dependency-free JSON encoder for the small change maps we produce. */
    private static String toCompactJson(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) return null;
        Map<String, Object> sorted = new TreeMap<>(changes);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":");
            appendValue(sb, e.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) { sb.append("null"); return; }
        if (value instanceof Number || value instanceof Boolean) { sb.append(value); return; }
        if (value instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append("\":");
                appendValue(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        sb.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
