package com.cartonapp.model;

import java.math.BigDecimal;

/** Customer record with a query-time calculated balance. */
public record CustomerSummary(Customer customer, BigDecimal balance) { }
