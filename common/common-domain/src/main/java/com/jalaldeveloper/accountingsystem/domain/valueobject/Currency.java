package com.jalaldeveloper.accountingsystem.domain.valueobject;

public record Currency(String code, String symbol, int decimalPlaces) {
    public static Currency USD() {
        return new Currency("USD", "$", 2);
    }

    public static Currency IQD() {
        return new Currency("IQD", "د.ع", 3);
    }
}
