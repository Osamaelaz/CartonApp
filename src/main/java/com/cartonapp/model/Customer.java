package com.cartonapp.model;

import java.math.BigDecimal;

/**
 * Persisted customer data. {@code openingBalance} is the debt entered by hand when the customer is
 * added (money owed from before using the program); the current balance is calculated separately
 * from it plus invoices minus receipts.
 */
public record Customer(long id, String code, String name, String industry, String phone,
        String address, BigDecimal openingBalance) {
    public Customer {
        openingBalance = openingBalance == null ? BigDecimal.ZERO : openingBalance;
    }

    public Customer(long id, String code, String name, String industry, String phone, String address) {
        this(id, code, name, industry, phone, address, BigDecimal.ZERO);
    }
}
