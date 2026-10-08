package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.InspectionEntity;
import com.bradox.erp.repair.service.domain.dto.OrderActivity;
import com.bradox.erp.repair.service.domain.ports.output.repository.OrderActivityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderActivityRepositoryImpl implements OrderActivityRepository {

    private final EntityManager em;

    public OrderActivityRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    private static final class Acc {
        int lines;
        int findings;
        boolean inspected;
        boolean signedOff;
        Instant last = Instant.EPOCH;

        void touch(Instant at) {
            if (at != null && at.isAfter(last)) {
                last = at;
            }
        }
    }

    @Override
    public List<OrderActivity> recent(CompanyId companyId, int limit) {
        UUID c = companyId.getId();
        Map<UUID, Acc> byOrder = new HashMap<>();
        for (Object[] r : em.createQuery("select l.orderId, count(l), max(l.createdAt) from LineEntity l where l.companyId = :c group by l.orderId",
                Object[].class).setParameter("c", c).getResultList()) {
            Acc a = byOrder.computeIfAbsent((UUID) r[0], k -> new Acc());
            a.lines = ((Number) r[1]).intValue();
            a.touch((Instant) r[2]);
        }
        for (Object[] r : em.createQuery("select f.orderId, count(f), max(f.createdAt) from FindingEntity f where f.companyId = :c group by f.orderId",
                Object[].class).setParameter("c", c).getResultList()) {
            Acc a = byOrder.computeIfAbsent((UUID) r[0], k -> new Acc());
            a.findings = ((Number) r[1]).intValue();
            a.touch((Instant) r[2]);
        }
        for (InspectionEntity i : em.createQuery("select i from InspectionEntity i where i.companyId = :c", InspectionEntity.class)
                .setParameter("c", c).getResultList()) {
            Acc a = byOrder.computeIfAbsent(i.orderId, k -> new Acc());
            a.inspected = true;
            a.signedOff = i.signedOffAt != null;
            a.touch(i.startedAt);
            a.touch(i.signedOffAt);
        }
        return byOrder.entrySet().stream()
                .map(e -> new OrderActivity(e.getKey(), e.getValue().lines, e.getValue().findings, e.getValue().inspected,
                        e.getValue().signedOff, e.getValue().last))
                .sorted(Comparator.comparing(OrderActivity::lastActivity).reversed())
                .limit(limit).toList();
    }
}
