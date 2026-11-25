package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Additive, idempotent schema changes required by the sales-invoice template. They only add
 * columns and indexes; no existing data is removed. They run once per application start before
 * the first invoice or balance query, and are also part of {@link SchemaInitializerDao}.
 */
final class SchemaUpgrades {
    static final List<String> INVOICE_TEMPLATE = List.of(
            // Older databases were created before unit_name existed; invoice loading needs it.
            "ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS unit_name VARCHAR(80)",
            // Debt entered by hand when a customer is added; part of every balance and previous balance.
            "ALTER TABLE customers ADD COLUMN IF NOT EXISTS opening_balance NUMERIC(14,2) NOT NULL DEFAULT 0",
            // Paper specifications ticked on the product form (كرافت، فلوت، تيست), comma separated.
            "ALTER TABLE products ADD COLUMN IF NOT EXISTS paper_specs VARCHAR(120) NOT NULL DEFAULT ''",
            "ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0",
            "ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS item_name VARCHAR(250)",
            "ALTER TABLE invoices ADD COLUMN IF NOT EXISTS additional_discount NUMERIC(14,2) NOT NULL DEFAULT 0",
            "ALTER TABLE receipts ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'",
            "CREATE INDEX IF NOT EXISTS idx_receipts_invoice ON receipts(invoice_id)",
            // Company details and the login password hash, edited from the settings screen.
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                setting_key VARCHAR(80) PRIMARY KEY,
                setting_value TEXT,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

    private static volatile boolean applied;

    private SchemaUpgrades() { }

    static void ensure(DatabaseConnectionFactory connections) throws SQLException {
        if (applied) return;
        synchronized (SchemaUpgrades.class) {
            if (applied) return;
            try (Connection connection = connections.open()) {
                connection.setAutoCommit(false);
                try {
                    for (String statement : INVOICE_TEMPLATE) {
                        try (PreparedStatement prepared = connection.prepareStatement(statement)) {
                            prepared.execute();
                        }
                    }
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            applied = true;
        }
    }
}
