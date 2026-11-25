package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One saved invoice with all data needed by the official sales-invoice template. The previous
 * balance is calculated from the customer's earlier invoices and receipts, so it reflects the
 * invoice's own position in the account history rather than the current balance.
 */
public record InvoiceDocument(long id, String number, LocalDate date, String status, Customer customer,
        List<InvoiceDocumentLine> lines, BigDecimal additionalDiscount, BigDecimal cashPaid,
        BigDecimal previousBalance) {
    public InvoiceDocument {
        lines = lines == null ? List.of() : List.copyOf(lines);
        additionalDiscount = additionalDiscount == null ? BigDecimal.ZERO : additionalDiscount;
        cashPaid = cashPaid == null ? BigDecimal.ZERO : cashPaid;
        previousBalance = previousBalance == null ? BigDecimal.ZERO : previousBalance;
    }

    public List<InvoiceLine> invoiceLines() {
        return lines.stream().map(InvoiceDocumentLine::line).toList();
    }
}
