# PostgreSQL preparation

The application now contains a local PostgreSQL configuration loader, a JDBC connection factory, and an idempotent schema initializer. The UI still uses the existing sample services; it does not attempt a database connection at startup.

## Local setup

1. Create a local PostgreSQL database named `carton_accounting`.
2. Copy `db.properties.example` to `db.properties` in the project root.
3. Set the local `db.url`, `db.user`, and `db.password`. Do not commit this file.
4. During the database integration stage, create a `DatabaseConnectionFactory` from `DatabaseConfig.loadDefault()` and invoke `SchemaInitializerDao.initialize()` from a `SwingWorker`, never on the Swing EDT.

## Schema included

- `customers`
- `products` and `product_prices`
- `invoices`, `invoice_lines`, `receipts`

Invoice lines support multiple items through `product_id`, `quantity`, `unit_name`, and
`unit_price`; invoice totals are calculated from those lines. `SchemaInitializerDao`
adds `invoice_lines.unit_name` with `IF NOT EXISTS`, so existing databases and rows
remain intact (older rows fall back to their existing description as the unit name).

Sales-invoice template columns (added automatically and idempotently on first use by `SchemaUpgrades`,
also included in `SchemaInitializerDao`; no data is removed):

- `app_settings(setting_key, setting_value, updated_at)`: company details and the login password hash (never plain text).
- `products.paper_specs` VARCHAR(120) default '': ticked paper specifications (كرافت,فلوت,تيست).
- `customers.opening_balance` NUMERIC(14,2) default 0: المديونية السابقة entered by hand when a customer is added.
  Customer debt = opening_balance + posted invoices − active receipts; it is part of every invoice's previous balance
  and appears as the first statement row.
- `invoice_lines.unit_name` VARCHAR(80): added automatically for databases created before it existed.
- `invoice_lines.discount_amount` NUMERIC(14,2) default 0: قيمة الخصم per line.
- `invoice_lines.item_name` VARCHAR(250): product name at the time the invoice was saved.
- `invoices.additional_discount` NUMERIC(14,2) default 0: خصم إضافي.
- `receipts.source` VARCHAR(20) default 'MANUAL': 'INVOICE' marks the receipt created for المدفوع نقداً on an invoice.

An invoice adds `Σ(ROUND(quantity × unit_price, 2) − discount_amount) − additional_discount` to the customer's debt.
Cash paid on an invoice is an ordinary active receipt linked to it, so balances and statements include it. Deleting an
invoice sets it and its invoice receipt to CANCELLED.

Invoice totals, paid amounts, remaining amounts, and customer balances are intentionally absent: they must be calculated from invoice lines and receipts when queried, as required by the project rules.

No default credentials or automatic code-generation rule is included. Seed data and the current pricing formula are defined by the application services.

Product pricing is now derived from sheet dimensions. The sheet length is
`(carton length + carton width) × 2`, the sheet width is `carton width + carton
height`, and the current base calculation uses the average three-layer cost,
6500 EGP manufacturing cost per ton, 3.3 corrugation factor, and average
grammage plus 7 grams. Legacy flute, paper-type, glue-flap, and trim fields are
removed by `sql/migrate-remove-product-pricing-fields.sql`.

Customer records contain only code, name, industry, phone, and address. The migrations
`sql/migrate-remove-customer-fields.sql` and `sql/migrate-remove-customer-credit-limit.sql`
remove legacy customer fields. Customers can be physically deleted only when no invoices or
receipts reference them.
