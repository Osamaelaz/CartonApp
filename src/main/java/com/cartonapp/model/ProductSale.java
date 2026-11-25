package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One posted invoice line that sold a product (for the product report). */
public record ProductSale(String invoiceNumber, LocalDate date, String customerName, BigDecimal quantity,
        BigDecimal unitPrice, BigDecimal discount) { }
