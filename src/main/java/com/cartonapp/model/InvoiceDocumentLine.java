package com.cartonapp.model;

/** An invoice line together with the item name printed in the الصنف column. */
public record InvoiceDocumentLine(InvoiceLine line, String itemName) {
    public InvoiceDocumentLine {
        itemName = itemName == null ? "" : itemName;
    }
}
