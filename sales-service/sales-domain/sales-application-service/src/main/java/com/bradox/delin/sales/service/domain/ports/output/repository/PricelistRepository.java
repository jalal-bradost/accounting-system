package com.bradox.delin.sales.service.domain.ports.output.repository;

import com.bradox.delin.sales.domain.core.entity.Pricelist;

import java.util.Optional;
import java.util.UUID;

public interface PricelistRepository {

    Optional<Pricelist> findById(UUID id);

    Optional<Pricelist> findByIdWithItems(UUID id);
}
