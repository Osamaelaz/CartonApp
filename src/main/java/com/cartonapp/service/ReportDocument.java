package com.cartonapp.service;

import java.util.List;

/**
 * Content of one exported report (customer list, one customer's account, product list, one
 * product). The same document is written to Excel and to PDF by {@link ReportExportService}, so both
 * files always contain identical data.
 *
 * <p>Values may be {@code String}, {@code java.math.BigDecimal} (money, two decimals),
 * {@code Number} or {@code java.time.LocalDate}.
 */
public record ReportDocument(String fileName, String title, String subject, List<Field> info,
        List<Field> summary, List<Column> columns, List<List<Object>> rows, List<Object> totals) {

    public ReportDocument {
        info = info == null ? List.of() : List.copyOf(info);
        summary = summary == null ? List.of() : List.copyOf(summary);
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
        totals = totals == null ? List.of() : totals;
    }

    /** A label / value pair shown above the table. */
    public record Field(String label, Object value) { }

    public enum Kind { TEXT, CENTER, MONEY, NUMBER }

    /** A table column; {@code weight} sets its share of the page width. */
    public record Column(String heading, Kind kind, float weight) { }
}
