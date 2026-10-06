package com.bradox.erp.bootstrap;

import com.bradox.erp.platform.settings.CompanyErpBootstrapHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CompanyErpBootstrapHandlerImpl implements CompanyErpBootstrapHandler {

    private final CompanyErpBootstrap companyErpBootstrap;

    public CompanyErpBootstrapHandlerImpl(CompanyErpBootstrap companyErpBootstrap) {
        this.companyErpBootstrap = companyErpBootstrap;
    }

    @Override
    public void bootstrap(UUID companyId) {
        companyErpBootstrap.bootstrap(companyId);
    }
}
