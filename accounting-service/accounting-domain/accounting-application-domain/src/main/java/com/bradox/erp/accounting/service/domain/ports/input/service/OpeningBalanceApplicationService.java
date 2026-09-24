package com.bradox.erp.accounting.service.domain.ports.input.service;

import com.bradox.erp.accounting.service.domain.create.OpeningBalanceCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceResponse;

import jakarta.validation.Valid;

public interface OpeningBalanceApplicationService {

    OpeningBalanceResponse setOpeningBalances(@Valid OpeningBalanceCommand command);
}
