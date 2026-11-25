package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Persisted customer receipt. Amounts are calculated into statements, not balances. */
public record Receipt(long id, String number, String customerCode, Long invoiceId,
        LocalDate date, BigDecimal amount, String paymentMethod, String reference, String status) {
}
