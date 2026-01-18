package com.jalaldeveloper.accountingsystem.application.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ErrorDTO{
    private String code;
    private String message;
}