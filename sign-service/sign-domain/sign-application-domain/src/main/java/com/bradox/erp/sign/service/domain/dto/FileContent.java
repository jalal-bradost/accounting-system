package com.bradox.erp.sign.service.domain.dto;

public record FileContent(byte[] bytes, String fileName, String contentType) {
}
