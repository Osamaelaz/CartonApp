package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.model.Customer;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceDocumentLine;
import com.cartonapp.model.InvoiceInput;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceSummary;
import com.cartonapp.model.Receipt;
import com.cartonapp.model.StatementEntry;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Invoice SQL. Invoice amounts are always calculated from lines, line discounts, the additional
 * discount and receipts; no total or balance is stored.
 */
public final class InvoiceDao {
    /** Receipt created together with an invoice for the template's "المدفوع نقداً" amount. */
    static final String INVOICE_CASH_SOURCE = "INVOICE";
    static final String MANUAL_SOURCE = "MANUAL";
    private static final String INVOICE_CASH_METHOD = "نقدي";

    private final DatabaseConnectionFactory connections;

    public InvoiceDao(DatabaseConnectionFactory connections) { this.connections = connections; }

    /**
     * SQL expression for the amount an invoice adds to the customer's debt:
     * sum of (ROUND(quantity × unit price, 2) − line discount) − additional discount.
     */
    static String invoiceDue(String invoiceAlias) {
        return "(COALESCE((SELECT SUM(ROUND(dl.quantity*dl.unit_price,2)-dl.discount_amount) FROM invoice_lines dl"
                + " WHERE dl.invoice_id=" + invoiceAlias + ".id),0)-" + invoiceAlias + ".additional_discount)";
    }

    /** Active (not cancelled) invoices, newest first. Cancelled invoices are deleted from the user's view. */
    public List<InvoiceSummary> findAll() throws SQLException {
        SchemaUpgrades.ensure(connections);
        return querySummaries("WHERE i.status<>'CANCELLED'", null);
    }

    private List<InvoiceSummary> querySummaries(String where, Long id) throws SQLException {
        String sql = "SELECT i.id,COALESCE(i.invoice_number,'—') AS number,c.code AS customer_code,"
                + "c.name AS customer_name,i.invoice_date,i.status," + invoiceDue("i") + " AS total,"
                + "COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.invoice_id=i.id AND r.status='ACTIVE'),0) AS paid "
                + "FROM invoices i JOIN customers c ON c.id=i.customer_id " + where
                + " ORDER BY i.invoice_date DESC,i.id DESC";
        List<InvoiceSummary> rows = new ArrayList<>();
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setQueryTimeout(15);
            if (id != null) p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) rows.add(map(r));
            }
        }
        return rows;
    }

    /** Creates the invoice, its lines and its cash receipt in one transaction. */
    public InvoiceSummary create(InvoiceInput input) throws SQLException {
        SchemaUpgrades.ensure(connections);
        long invoiceId;
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                long customerId = customerId(c, input.customerCode());
                String number = nextNumber(c, "invoices", "INV", input.date());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO invoices(invoice_number,customer_id,invoice_date,status,additional_discount) "
                                + "VALUES (?,?,?,'POSTED',?) RETURNING id")) {
                    p.setString(1, number);
                    p.setLong(2, customerId);
                    p.setDate(3, java.sql.Date.valueOf(input.date()));
                    p.setBigDecimal(4, input.additionalDiscount());
                    try (ResultSet r = p.executeQuery()) {
                        r.next();
                        invoiceId = r.getLong(1);
                    }
                }
                replaceLines(c, invoiceId, input.lines());
                syncCashReceipt(c, invoiceId, number, customerId, input.date(), input.cashPaid());
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
        return querySummaries("WHERE i.id=?", invoiceId).getFirst();
    }

    /** Updates an issued invoice. The invoice number never changes. */
    public void update(long invoiceId, InvoiceInput input) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                long customerId = customerId(c, input.customerCode());
                String number;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT invoice_number,status FROM invoices WHERE id=? FOR UPDATE")) {
                    p.setLong(1, invoiceId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("الفاتورة غير موجودة");
                        if ("CANCELLED".equals(r.getString("status")))
                            throw new SQLException("لا يمكن تعديل فاتورة محذوفة");
                        number = r.getString("invoice_number");
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE invoices SET customer_id=?,invoice_date=?,additional_discount=?,"
                                + "updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                    p.setLong(1, customerId);
                    p.setDate(2, java.sql.Date.valueOf(input.date()));
                    p.setBigDecimal(3, input.additionalDiscount());
                    p.setLong(4, invoiceId);
                    p.executeUpdate();
                }
                replaceLines(c, invoiceId, input.lines());
                syncCashReceipt(c, invoiceId, number, customerId, input.date(), input.cashPaid());
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Loads everything the invoice template prints for one invoice. */
    public InvoiceDocument findDocument(long invoiceId) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String header = """
                SELECT i.id,COALESCE(i.invoice_number,'—') AS number,i.invoice_date,i.status,i.created_at,
                       i.additional_discount,c.id AS customer_id,c.code,c.name,c.industry,c.phone,c.address,
                       COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.invoice_id=i.id
                                 AND r.source='INVOICE' AND r.status='ACTIVE'),0) AS cash_paid
                FROM invoices i JOIN customers c ON c.id=i.customer_id WHERE i.id=?
                """;
        String linesSql = """
                SELECT l.id,l.line_number,l.product_id,l.quantity,l.unit_name,l.unit_price,l.discount_amount,
                       l.description,l.item_name,p.name AS product_name
                FROM invoice_lines l LEFT JOIN products p ON p.id=l.product_id
                WHERE l.invoice_id=? ORDER BY l.line_number
                """;
        try (Connection c = connections.open()) {
            long customerId;
            String number;
            String status;
            LocalDate date;
            Timestamp createdAt;
            BigDecimal additionalDiscount;
            BigDecimal cashPaid;
            Customer customer;
            try (PreparedStatement p = c.prepareStatement(header)) {
                p.setLong(1, invoiceId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new SQLException("الفاتورة غير موجودة");
                    customerId = r.getLong("customer_id");
                    number = r.getString("number");
                    status = r.getString("status");
                    date = r.getDate("invoice_date").toLocalDate();
                    createdAt = r.getTimestamp("created_at");
                    additionalDiscount = r.getBigDecimal("additional_discount");
                    cashPaid = r.getBigDecimal("cash_paid");
                    customer = new Customer(customerId, r.getString("code"), r.getString("name"),
                            r.getString("industry"), r.getString("phone"), r.getString("address"));
                }
            }
            List<InvoiceDocumentLine> lines = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(linesSql)) {
                p.setLong(1, invoiceId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        InvoiceLine line = mapLine(r);
                        String name = firstPresent(r.getString("item_name"), r.getString("product_name"),
                                r.getString("description"));
                        lines.add(new InvoiceDocumentLine(line, name));
                    }
                }
            }
            BigDecimal previous = previousBalance(c, customerId, date, invoiceId, createdAt);
            return new InvoiceDocument(invoiceId, number, date, status, customer, lines, additionalDiscount,
                    cashPaid, previous);
        }
    }

    /**
     * Customer balance before an invoice dated {@code date}. For a saved invoice, earlier invoices
     * on the same date (lower id) and receipts recorded before it on that date are included; the
     * invoice itself and its own cash receipt are excluded.
     */
    public BigDecimal findPreviousBalance(String customerCode, LocalDate date, Long invoiceId) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c = connections.open()) {
            long customerId = customerId(c, customerCode);
            Timestamp createdAt = null;
            if (invoiceId != null) {
                try (PreparedStatement p = c.prepareStatement("SELECT created_at FROM invoices WHERE id=?")) {
                    p.setLong(1, invoiceId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) createdAt = r.getTimestamp(1);
                    }
                }
            }
            return previousBalance(c, customerId, date, invoiceId, createdAt);
        }
    }

    private static BigDecimal previousBalance(Connection c, long customerId, LocalDate date, Long invoiceId,
            Timestamp createdAt) throws SQLException {
        String sql = """
                WITH params AS (
                    SELECT CAST(? AS BIGINT) AS customer_id, CAST(? AS DATE) AS d,
                           CAST(? AS BIGINT) AS x, CAST(? AS TIMESTAMP) AS t
                ), prior_invoices AS (
                    SELECT i.id FROM invoices i, params p
                    WHERE i.customer_id=p.customer_id AND i.status='POSTED'
                      AND (p.x IS NULL OR i.id<>p.x)
                      AND (i.invoice_date < p.d OR (i.invoice_date = p.d AND (p.x IS NULL OR i.id < p.x)))
                )
                SELECT (SELECT cu.opening_balance FROM customers cu, params p WHERE cu.id=p.customer_id)
                     + COALESCE((SELECT SUM(""" + invoiceDue("i") + """
                       ) FROM invoices i WHERE i.id IN (SELECT id FROM prior_invoices)),0)
                     - COALESCE((SELECT SUM(r.amount) FROM receipts r, params p
                        WHERE r.customer_id=p.customer_id AND r.status='ACTIVE'
                          AND ((r.source='INVOICE' AND r.invoice_id IN (SELECT id FROM prior_invoices))
                            OR (r.source<>'INVOICE' AND (r.receipt_date < p.d
                                OR (r.receipt_date = p.d AND (p.t IS NULL OR r.created_at < p.t)))))),0)
                       AS balance
                """;
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, customerId);
            p.setDate(2, java.sql.Date.valueOf(date));
            if (invoiceId == null) p.setNull(3, Types.BIGINT); else p.setLong(3, invoiceId);
            if (createdAt == null) p.setNull(4, Types.TIMESTAMP); else p.setTimestamp(4, createdAt);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getBigDecimal("balance");
            }
        }
    }

    public List<InvoiceLine> findDetails(long invoiceId) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "SELECT id,line_number,product_id,quantity,unit_name,unit_price,discount_amount,description "
                + "FROM invoice_lines WHERE invoice_id=? ORDER BY line_number";
        List<InvoiceLine> lines = new ArrayList<>();
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, invoiceId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) lines.add(mapLine(r));
            }
        }
        return lines;
    }

    private static InvoiceLine mapLine(ResultSet r) throws SQLException {
        String unit = firstPresent(r.getString("unit_name"), r.getString("description"));
        return new InvoiceLine(r.getLong("id"), r.getInt("line_number"), (Long) r.getObject("product_id"),
                r.getBigDecimal("quantity"), unit, r.getBigDecimal("unit_price"), r.getBigDecimal("discount_amount"));
    }

    private static void replaceLines(Connection c, long invoiceId, List<InvoiceLine> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) throw new SQLException("الفاتورة يجب أن تحتوي على صنف واحد على الأقل");
        try (PreparedStatement delete = c.prepareStatement("DELETE FROM invoice_lines WHERE invoice_id=?")) {
            delete.setLong(1, invoiceId);
            delete.executeUpdate();
        }
        // item_name snapshots the product name so a printed invoice keeps the name it was issued with.
        String sql = "INSERT INTO invoice_lines(invoice_id,line_number,product_id,description,item_name,quantity,"
                + "unit_name,unit_price,discount_amount) "
                + "VALUES (?,?,?,?,(SELECT name FROM products WHERE id=?),?,?,?,?)";
        try (PreparedStatement insert = c.prepareStatement(sql)) {
            int number = 1;
            for (InvoiceLine line : lines) {
                String unit = line.unitName().isBlank() ? "قطعة" : line.unitName();
                insert.setLong(1, invoiceId);
                insert.setInt(2, number++);
                if (line.productId() == null) {
                    insert.setNull(3, Types.BIGINT);
                    insert.setNull(5, Types.BIGINT);
                } else {
                    insert.setLong(3, line.productId());
                    insert.setLong(5, line.productId());
                }
                insert.setString(4, unit);
                insert.setBigDecimal(6, line.quantity());
                insert.setString(7, unit);
                insert.setBigDecimal(8, line.unitPrice());
                insert.setBigDecimal(9, line.discount());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /**
     * Keeps exactly one active invoice-cash receipt matching {@code cash}. A zero amount cancels the
     * receipt instead of deleting it, so the accounting history remains intact.
     */
    private static void syncCashReceipt(Connection c, long invoiceId, String invoiceNumber, long customerId,
            LocalDate date, BigDecimal cash) throws SQLException {
        List<Long> existing = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM receipts WHERE invoice_id=? AND source=? "
                + "AND status='ACTIVE' ORDER BY id FOR UPDATE")) {
            p.setLong(1, invoiceId);
            p.setString(2, INVOICE_CASH_SOURCE);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) existing.add(r.getLong(1));
            }
        }
        boolean hasCash = cash != null && cash.signum() > 0;
        int keep = 0;
        if (hasCash && !existing.isEmpty()) {
            try (PreparedStatement p = c.prepareStatement("UPDATE receipts SET customer_id=?,receipt_date=?,amount=?,"
                    + "reference=? WHERE id=?")) {
                p.setLong(1, customerId);
                p.setDate(2, java.sql.Date.valueOf(date));
                p.setBigDecimal(3, cash);
                p.setString(4, invoiceNumber);
                p.setLong(5, existing.getFirst());
                p.executeUpdate();
            }
            keep = 1;
        }
        for (int i = keep; i < existing.size(); i++) {
            try (PreparedStatement p = c.prepareStatement("UPDATE receipts SET status='CANCELLED' WHERE id=?")) {
                p.setLong(1, existing.get(i));
                p.executeUpdate();
            }
        }
        if (hasCash && existing.isEmpty()) {
            String number = nextNumber(c, "receipts", "RCPT", date);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO receipts(receipt_number,customer_id,invoice_id,"
                    + "receipt_date,amount,payment_method,reference,status,source) VALUES (?,?,?,?,?,?,?,'ACTIVE',?)")) {
                p.setString(1, number);
                p.setLong(2, customerId);
                p.setLong(3, invoiceId);
                p.setDate(4, java.sql.Date.valueOf(date));
                p.setBigDecimal(5, cash);
                p.setString(6, INVOICE_CASH_METHOD);
                p.setString(7, invoiceNumber);
                p.setString(8, INVOICE_CASH_SOURCE);
                p.executeUpdate();
            }
        }
    }

    /** Deletes an invoice for the user by cancelling it and its cash receipt in one transaction. */
    public void cancel(long id) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE invoices SET status='CANCELLED',updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                    p.setLong(1, id);
                    if (p.executeUpdate() == 0) throw new SQLException("الفاتورة غير موجودة");
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE receipts SET status='CANCELLED' WHERE invoice_id=? AND source=? AND status='ACTIVE'")) {
                    p.setLong(1, id);
                    p.setString(2, INVOICE_CASH_SOURCE);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public List<StatementEntry> findCustomerStatement(String customerCode) throws SQLException {
        return findCustomerStatement(customerCode, null, null);
    }

    public List<StatementEntry> findCustomerStatement(String customerCode, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = """
          SELECT movement_date,description,invoice_number,debit,credit,movement_id
          FROM (
            SELECT i.invoice_date AS movement_date,
                   'فاتورة مبيعات مرحلة' AS description,
                   COALESCE(i.invoice_number,'—') AS invoice_number,
                   """ + invoiceDue("i") + """
                    AS debit,
                   CAST(0 AS NUMERIC(14,2)) AS credit,
                   i.id AS movement_id,
                   0 AS movement_kind
            FROM invoices i
            JOIN customers c ON c.id=i.customer_id
            WHERE c.code=? AND i.status='POSTED'
              AND (CAST(? AS DATE) IS NULL OR i.invoice_date >= CAST(? AS DATE))
              AND (CAST(? AS DATE) IS NULL OR i.invoice_date <= CAST(? AS DATE))
            UNION ALL
            SELECT r.receipt_date AS movement_date,
                   'تحصيل — ' || r.payment_method AS description,
                   COALESCE(i.invoice_number,'—') AS invoice_number,
                   CAST(0 AS NUMERIC(14,2)) AS debit,
                   r.amount AS credit,
                   r.id AS movement_id,
                   1 AS movement_kind
            FROM receipts r
            JOIN customers c ON c.id=r.customer_id
            LEFT JOIN invoices i ON i.id=r.invoice_id
            WHERE c.code=? AND r.status='ACTIVE'
              AND (CAST(? AS DATE) IS NULL OR r.receipt_date >= CAST(? AS DATE))
              AND (CAST(? AS DATE) IS NULL OR r.receipt_date <= CAST(? AS DATE))
          ) movements
          ORDER BY movement_date,movement_kind,movement_id
          """;
        List<StatementEntry> entries = new ArrayList<>();
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, customerCode); p.setDate(2, sqlDate(startDate)); p.setDate(3, sqlDate(startDate));
            p.setDate(4, sqlDate(endDate)); p.setDate(5, sqlDate(endDate));
            p.setString(6, customerCode); p.setDate(7, sqlDate(startDate)); p.setDate(8, sqlDate(startDate));
            p.setDate(9, sqlDate(endDate)); p.setDate(10, sqlDate(endDate));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) entries.add(new StatementEntry(
                        r.getDate("movement_date").toLocalDate(), r.getString("description"),
                        r.getString("invoice_number"), r.getBigDecimal("debit"),
                        r.getBigDecimal("credit"), BigDecimal.ZERO));
            }
        }
        return entries;
    }

    /**
     * Customer balance before {@code from}: opening debt + posted invoices dated before it − active
     * receipts dated before it. With no date this is the opening debt alone.
     */
    public BigDecimal findBalanceBefore(String customerCode, LocalDate from) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "SELECT c.opening_balance"
                + " + COALESCE((SELECT SUM(" + invoiceDue("i") + ") FROM invoices i WHERE i.customer_id=c.id"
                + " AND i.status='POSTED' AND CAST(? AS DATE) IS NOT NULL AND i.invoice_date < CAST(? AS DATE)),0)"
                + " - COALESCE((SELECT SUM(r.amount) FROM receipts r WHERE r.customer_id=c.id AND r.status='ACTIVE'"
                + " AND CAST(? AS DATE) IS NOT NULL AND r.receipt_date < CAST(? AS DATE)),0) AS balance"
                + " FROM customers c WHERE c.code=?";
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) p.setDate(i, sqlDate(from));
            p.setString(5, customerCode);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("العميل غير موجود");
                return r.getBigDecimal("balance");
            }
        }
    }

    private static java.sql.Date sqlDate(LocalDate value) { return value == null ? null : java.sql.Date.valueOf(value); }

    public Receipt createReceipt(String customerCode, Long invoiceId, LocalDate date, BigDecimal amount,
            String paymentMethod, String reference) throws SQLException {
        SchemaUpgrades.ensure(connections);
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                long customerId = customerId(c, customerCode);
                String number = nextNumber(c, "receipts", "RCPT", date);
                String sql = "INSERT INTO receipts(receipt_number,customer_id,invoice_id,receipt_date,amount,"
                        + "payment_method,reference,status,source) VALUES (?,?,?,?,?,?,?,'ACTIVE',?) RETURNING id";
                long id;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setString(1, number); p.setLong(2, customerId);
                    if (invoiceId == null) p.setNull(3, Types.BIGINT); else p.setLong(3, invoiceId);
                    p.setDate(4, java.sql.Date.valueOf(date)); p.setBigDecimal(5, amount);
                    p.setString(6, paymentMethod); p.setString(7, reference); p.setString(8, MANUAL_SOURCE);
                    try (ResultSet r = p.executeQuery()) { r.next(); id = r.getLong(1); }
                }
                c.commit();
                return new Receipt(id, number, customerCode, invoiceId, date, amount, paymentMethod, reference, "ACTIVE");
            } catch (SQLException exception) {
                c.rollback();
                throw exception;
            }
        }
    }

    /** Existing numbering strategy: PREFIX-year-(max id + 1), protected by the UNIQUE constraint. */
    private static String nextNumber(Connection c, String table, String prefix, LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id),0)+1 FROM " + ("receipts".equals(table) ? "receipts" : "invoices");
        long sequence;
        try (PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            r.next();
            sequence = r.getLong(1);
        }
        if (!"invoices".equals(table))
            return String.format("%s-%d-%05d", prefix, date.getYear(), sequence);
        // Invoices are numbered INV-00001, INV-00002, ...; skip any number that is already taken.
        while (true) {
            String number = String.format("%s-%05d", prefix, sequence);
            try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM invoices WHERE invoice_number=?")) {
                p.setString(1, number);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return number;
                }
            }
            sequence++;
        }
    }

    private static long customerId(Connection c, String code) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM customers WHERE code=?")) {
            p.setString(1, code);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("العميل غير موجود");
                return r.getLong(1);
            }
        }
    }

    private static String firstPresent(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static InvoiceSummary map(ResultSet r) throws SQLException {
        return new InvoiceSummary(r.getLong("id"), r.getString("number"), r.getString("customer_code"),
                r.getString("customer_name"), r.getDate("invoice_date").toLocalDate(), r.getString("status"),
                r.getBigDecimal("total"), r.getBigDecimal("paid"));
    }
}
