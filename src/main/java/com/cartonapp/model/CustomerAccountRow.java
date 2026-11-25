package com.cartonapp.model;

/** A customer with its calculated account totals, for the customer-list report. */
public record CustomerAccountRow(Customer customer, CustomerAccount account) { }
