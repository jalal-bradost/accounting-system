package com.jalaldeveloper.accountingsystem.contacts.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.contacts.dataaccess.entity.PaymentTermsEntity;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.entity.PaymentTerms;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject.PaymentTermsId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

@Component
public class PaymentTermsDataAccessMapper {

    public PaymentTerms entityToDomain(PaymentTermsEntity e) {
        if (e == null) return null;
        PaymentTerms.Builder b = PaymentTerms.builder()
                .id(new PaymentTermsId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName())
                .daysNet(e.getDaysNet())
                .discountDays(e.getDiscountDays())
                .discountPercent(e.getDiscountPercent());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public PaymentTermsEntity domainToEntity(PaymentTerms d, PaymentTermsEntity existingOrNull) {
        if (d == null) return null;
        PaymentTermsEntity e = existingOrNull != null ? existingOrNull : new PaymentTermsEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setName(d.getName());
        e.setDaysNet(d.getDaysNet());
        e.setDiscountDays(d.getDiscountDays());
        e.setDiscountPercent(d.getDiscountPercent());
        e.setActive(d.isActive());
        e.setArchivedAt(d.getArchivedAt());
        e.setArchivedBy(d.getArchivedBy());
        return e;
    }
}
