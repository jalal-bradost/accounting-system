package com.bradox.erp.documents.service.domain.ports.output.repository;

import com.bradox.erp.documents.domain.core.entity.Document;

import java.util.List;

public record DocumentPage(List<Document> content, long totalElements) {
}
