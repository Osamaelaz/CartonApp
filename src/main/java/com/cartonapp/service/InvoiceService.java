package com.cartonapp.service;

import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceInput;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceSummary;
import com.cartonapp.model.Receipt;
import com.cartonapp.model.StatementEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {
    List<InvoiceSummary> findAll() throws Exception;

    /** Validates and creates an invoice with its lines and cash payment. */
    InvoiceSummary create(InvoiceInput input) throws Exception;

    /** Validates and updates an invoice; its number is preserved. */
    void update(long invoiceId, InvoiceInput input) throws Exception;

    InvoiceDocument findDocument(long invoiceId) throws Exception;

    /** Customer balance before an invoice on {@code date}; {@code invoiceId} is null for a new invoice. */
    BigDecimal findPreviousBalance(String customerCode, LocalDate date, Long invoiceId) throws Exception;

    List<InvoiceLine> findDetails(long invoiceId) throws Exception;

    void cancel(long id) throws Exception;

    List<StatementEntry> findCustomerStatement(String customerCode) throws Exception;

    List<StatementEntry> findCustomerStatement(String customerCode, LocalDate startDate, LocalDate endDate)
            throws Exception;

    Receipt createReceipt(String customerCode, Long invoiceId, LocalDate date, BigDecimal amount,
            String paymentMethod, String reference) throws Exception;
}
