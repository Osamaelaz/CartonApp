package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the accountant enters for one sales invoice: customer, date, lines (with line
 * discounts), the additional invoice discount and the cash paid when the invoice is issued.
 */
public record InvoiceInput(String customerCode, LocalDate date, List<InvoiceLine> lines,
        BigDecimal additionalDiscount, BigDecimal cashPaid) {
    public InvoiceInput {
        lines = lines == null ? List.of() : List.copyOf(lines);
        additionalDiscount = additionalDiscount == null ? BigDecimal.ZERO : additionalDiscount;
        cashPaid = cashPaid == null ? BigDecimal.ZERO : cashPaid;
    }
}
