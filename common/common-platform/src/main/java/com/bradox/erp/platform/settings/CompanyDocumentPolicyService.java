package com.bradox.erp.platform.settings;

import com.bradox.erp.platform.dataaccess.entity.CompanyEntity;
import com.bradox.erp.platform.dataaccess.repository.CompanyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reads company-level document policies (bill without receipt / invoice without delivery).
 */
@Service
public class CompanyDocumentPolicyService {

    private final CompanyJpaRepository companyRepository;

    public CompanyDocumentPolicyService(CompanyJpaRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public boolean allowBillWithoutReceipt(UUID companyId) {
        if (companyId == null) {
            return false;
        }
        return companyRepository.findById(companyId)
                .map(CompanyEntity::isAllowBillWithoutReceipt)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean allowInvoiceWithoutDelivery(UUID companyId) {
        if (companyId == null) {
            return false;
        }
        return companyRepository.findById(companyId)
                .map(CompanyEntity::isAllowInvoiceWithoutDelivery)
                .orElse(false);
    }
}
