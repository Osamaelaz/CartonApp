package com.cartonapp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Readable Arabic file names for exports, e.g. "فاتورة الصفوة للأجهزة المنزلية 2026-11-14.pdf" or
 * "كشف حساب النيل للصناعات الغذائية 2026-11-14.xlsx". Characters Windows does not allow are
 * removed, and an existing file is never overwritten: " (2)", " (3)" … are added instead.
 */
public final class ExportFiles {
    public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ExportFiles() { }

    public static String date(LocalDate day) {
        return DAY.format(day == null ? LocalDate.now() : day);
    }

    /** "فاتورة <العميل> <تاريخ الإصدار>". */
    public static String invoiceName(String customerName, LocalDate issueDate) {
        return "فاتورة " + text(customerName) + " " + date(issueDate);
    }

    /** "كشف حساب <العميل> <تاريخ الإصدار>". */
    public static String statementName(String customerName, LocalDate issueDate) {
        return "كشف حساب " + text(customerName) + " " + date(issueDate);
    }

    /** "<المنتج> - <العميل>". */
    public static String productName(String productName, String customerName) {
        return text(productName) + " - " + text(customerName);
    }

    /** A free path in the exports folder for this name and extension (".pdf", ".xlsx"). */
    public static Path path(String name, String extension) throws IOException {
        Path exports = com.cartonapp.config.AppPaths.exportsDir();
        Files.createDirectories(exports);
        String base = clean(name);
        Path candidate = exports.resolve(base + extension);
        for (int copy = 2; Files.exists(candidate); copy++)
            candidate = exports.resolve(base + " (" + copy + ")" + extension);
        return candidate;
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "بدون اسم" : value.trim();
    }

    private static String clean(String name) {
        String cleaned = (name == null ? "" : name).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ")
                .replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) cleaned = "تقرير";
        return cleaned.length() > 120 ? cleaned.substring(0, 120).trim() : cleaned;
    }
}
