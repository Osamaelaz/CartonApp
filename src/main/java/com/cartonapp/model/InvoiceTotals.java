package com.cartonapp.model;

import java.math.BigDecimal;

/** Calculated invoice summary exactly as laid out in the sales-invoice template. */
public record InvoiceTotals(BigDecimal totalQuantity, BigDecimal invoiceValue, BigDecimal previousBalance,
        BigDecimal additionalDiscount, BigDecimal totalWithDebt, BigDecimal cashPaid, BigDecimal finalBalance) {
}
