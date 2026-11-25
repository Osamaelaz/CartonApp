package com.cartonapp.service;

import com.cartonapp.dao.InvoiceDao;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceInput;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceSummary;
import com.cartonapp.model.Receipt;
import com.cartonapp.model.StatementEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DatabaseInvoiceService implements InvoiceService {
    private final InvoiceDao dao;
    private final InvoiceValidationService validation = new InvoiceValidationService();

    public DatabaseInvoiceService(InvoiceDao dao) { this.dao = dao; }

    public List<InvoiceSummary> findAll() throws Exception { return dao.findAll(); }

    public InvoiceSummary create(InvoiceInput input) throws Exception {
        validation.requireValid(input);
        return dao.create(normalise(input));
    }

    public void update(long invoiceId, InvoiceInput input) throws Exception {
        validation.requireValid(input);
        dao.update(invoiceId, normalise(input));
    }

    public InvoiceDocument findDocument(long invoiceId) throws Exception { return dao.findDocument(invoiceId); }

    public BigDecimal findPreviousBalance(String customerCode, LocalDate date, Long invoiceId) throws Exception {
        return dao.findPreviousBalance(customerCode, date, invoiceId);
    }

    public List<InvoiceLine> findDetails(long invoiceId) throws Exception { return dao.findDetails(invoiceId); }

    public void cancel(long id) throws Exception { dao.cancel(id); }

    public List<StatementEntry> findCustomerStatement(String customerCode) throws Exception {
        return findCustomerStatement(customerCode, null, null);
    }

    /**
     * Statement for a period. The first row carries the balance brought forward (opening debt plus
     * all movements before the period), so the last row's balance equals the customer's debt.
     */
    public List<StatementEntry> findCustomerStatement(String customerCode, LocalDate startDate, LocalDate endDate)
            throws Exception {
        BigDecimal opening = dao.findBalanceBefore(customerCode, startDate);
        List<StatementEntry> movements = dao.findCustomerStatement(customerCode, startDate, endDate);
        java.util.ArrayList<StatementEntry> rows = new java.util.ArrayList<>();
        if (startDate != null || opening.signum() != 0) {
            LocalDate day = startDate != null ? startDate
                    : movements.isEmpty() ? LocalDate.now() : movements.getFirst().date();
            String text = startDate == null ? "رصيد افتتاحي (مديونية سابقة)" : "رصيد سابق مُرحّل";
            rows.add(new StatementEntry(day, text, "—", opening.max(BigDecimal.ZERO),
                    opening.min(BigDecimal.ZERO).negate(), BigDecimal.ZERO));
        }
        rows.addAll(movements);
        return StatementBalanceCalculator.withBalances(rows);
    }

    public Receipt createReceipt(String customerCode, Long invoiceId, LocalDate date, BigDecimal amount,
            String paymentMethod, String reference) throws Exception {
        return dao.createReceipt(customerCode, invoiceId, date, amount, paymentMethod, reference);
    }

    /** Stores values with exactly two decimals so database values equal the displayed ones. */
    private static InvoiceInput normalise(InvoiceInput input) {
        List<InvoiceLine> lines = input.lines().stream().map(line -> new InvoiceLine(line.id(), line.lineNumber(),
                line.productId(), InvoiceCalculator.money(line.quantity()), line.unitName(),
                InvoiceCalculator.money(line.unitPrice()), InvoiceCalculator.money(line.discount()))).toList();
        return new InvoiceInput(input.customerCode(), input.date(), lines,
                InvoiceCalculator.money(input.additionalDiscount()), InvoiceCalculator.money(input.cashPaid()));
    }
}
