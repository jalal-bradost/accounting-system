package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.service.domain.dto.OrderActivity;
import com.bradox.erp.repair.service.domain.ports.input.OrderApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.OrderActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class OrderApplicationServiceImpl implements OrderApplicationService {

    private final OrderActivityRepository activity;
    private final RepairAccess access;

    OrderApplicationServiceImpl(OrderActivityRepository activity, RepairAccess access) {
        this.activity = activity;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderActivity> recent(CompanyId companyId) {
        access.require(RepairPermissions.ORDER_VIEW);
        return activity.recent(companyId, 200);
    }
}
