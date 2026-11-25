package com.cartonapp.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single invoice item as stored in {@code invoice_lines}. Line value and net amount are
 * calculated by {@code InvoiceCalculator}; they are never stored.
 */
public record InvoiceLine(long id, int lineNumber, Long productId, BigDecimal quantity,
        String unitName, BigDecimal unitPrice, BigDecimal discount) {

    public InvoiceLine {
        Objects.requireNonNull(quantity, "quantity");
        unitName = unitName == null ? "" : unitName;
        Objects.requireNonNull(unitPrice, "unitPrice");
        discount = discount == null ? BigDecimal.ZERO : discount;
    }

    public InvoiceLine(long id, int lineNumber, Long productId, BigDecimal quantity,
            String unitName, BigDecimal unitPrice) {
        this(id, lineNumber, productId, quantity, unitName, unitPrice, BigDecimal.ZERO);
    }

    public InvoiceLine(Long productId, BigDecimal quantity, String unitName, BigDecimal unitPrice) {
        this(0, 0, productId, quantity, unitName, unitPrice, BigDecimal.ZERO);
    }
}
