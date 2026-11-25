package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** Creates the local application schema. All database SQL remains in the DAO layer. */
public final class SchemaInitializerDao {
    private static final List<String> STATEMENTS = java.util.stream.Stream.concat(List.of(
            """
            CREATE TABLE IF NOT EXISTS customers (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                code VARCHAR(32) NOT NULL UNIQUE,
                name VARCHAR(200) NOT NULL,
                industry VARCHAR(150),
                phone VARCHAR(40),
                address TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                code VARCHAR(32) NOT NULL UNIQUE,
                customer_id BIGINT NOT NULL REFERENCES customers(id),
                name VARCHAR(200) NOT NULL,
                box_type VARCHAR(120) NOT NULL,
                length_cm NUMERIC(10,2) NOT NULL,
                width_cm NUMERIC(10,2) NOT NULL,
                height_cm NUMERIC(10,2) NOT NULL,
                layer_count INTEGER NOT NULL,
                print_colors INTEGER NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS product_prices (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                effective_date DATE NOT NULL,
                unit_price NUMERIC(14,2) NOT NULL,
                UNIQUE (product_id, effective_date)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS invoices (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                invoice_number VARCHAR(40) UNIQUE,
                customer_id BIGINT NOT NULL REFERENCES customers(id),
                invoice_date DATE NOT NULL,
                due_date DATE,
                status VARCHAR(32) NOT NULL,
                discount_rate NUMERIC(7,4) NOT NULL DEFAULT 0,
                tax_rate NUMERIC(7,4) NOT NULL DEFAULT 0,
                notes TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS invoice_lines (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
                line_number INTEGER NOT NULL,
                product_id BIGINT REFERENCES products(id),
                description VARCHAR(250) NOT NULL,
                quantity NUMERIC(14,2) NOT NULL,
                unit_price NUMERIC(14,2) NOT NULL,
                UNIQUE (invoice_id, line_number)
            )
            """,
            // Added separately so databases created by earlier versions are upgraded safely.
            "ALTER TABLE customers DROP COLUMN IF EXISTS tax_number",
            "ALTER TABLE customers DROP COLUMN IF EXISTS payment_days",
            "ALTER TABLE customers DROP COLUMN IF EXISTS tax_exempt",
            "ALTER TABLE customers DROP COLUMN IF EXISTS active",
            "ALTER TABLE customers DROP COLUMN IF EXISTS credit_limit",
            "ALTER TABLE products DROP COLUMN IF EXISTS flute_type_id",
            "ALTER TABLE products DROP COLUMN IF EXISTS glue_flap_cm",
            "ALTER TABLE products DROP COLUMN IF EXISTS trim_cm",
            "DROP TABLE IF EXISTS product_paper_layers",
            "DROP TABLE IF EXISTS flute_types",
            "DROP TABLE IF EXISTS paper_types",
            "DROP TABLE IF EXISTS pricing_components",
            "ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS unit_name VARCHAR(80)",
            """
            CREATE TABLE IF NOT EXISTS receipts (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                receipt_number VARCHAR(40) NOT NULL UNIQUE,
                customer_id BIGINT NOT NULL REFERENCES customers(id),
                invoice_id BIGINT REFERENCES invoices(id),
                receipt_date DATE NOT NULL,
                amount NUMERIC(14,2) NOT NULL,
                payment_method VARCHAR(40) NOT NULL,
                reference VARCHAR(120),
                status VARCHAR(32) NOT NULL,
                notes TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_products_customer ON products(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_product_prices_product_date ON product_prices(product_id, effective_date DESC)",
            "CREATE INDEX IF NOT EXISTS idx_invoices_customer_date ON invoices(customer_id, invoice_date)",
            "CREATE INDEX IF NOT EXISTS idx_receipts_customer_date ON receipts(customer_id, receipt_date)"
    ).stream(), SchemaUpgrades.INVOICE_TEMPLATE.stream()).toList();

    private final DatabaseConnectionFactory connections;

    public SchemaInitializerDao(DatabaseConnectionFactory connections) {
        this.connections = connections;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                for (String statement : STATEMENTS) {
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
    }
}
