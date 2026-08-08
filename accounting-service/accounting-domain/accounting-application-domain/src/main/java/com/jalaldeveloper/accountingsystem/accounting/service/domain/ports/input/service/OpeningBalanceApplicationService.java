package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceResponse;

import jakarta.validation.Valid;

public interface OpeningBalanceApplicationService {

    OpeningBalanceResponse setOpeningBalances(@Valid OpeningBalanceCommand command);
}
