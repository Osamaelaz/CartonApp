package com.cartonapp.service;

import java.util.List;

/** Thrown when an invoice cannot be saved; carries Arabic messages for the accountant. */
public final class InvoiceValidationException extends Exception {
    private final List<String> problems;

    public InvoiceValidationException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
