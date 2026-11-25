package com.cartonapp.model;

/** Actual counts shown on a customer's RTL sub-tabs. */
public record CustomerActivityCounts(int statementEntries, int products, int invoices) { }
