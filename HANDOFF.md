# Carton Accounting Project Handoff

## Project overview

Carton Accounting ("الليثي باك") is a Windows desktop application for one accountant at a corrugated-carton factory. It manages customers (with opening debt), carton products and prices, sales invoices, receipts and customer account statements. The UI is Arabic and fully right-to-left. Currency is Egyptian pound; money uses `BigDecimal` with two decimal places.

`AGENTS.md` remains authoritative; this handoff supplements it. `docs/PROGRESS.md` has the per-feature checklists and the list of differences from the original design.

## Technology stack

- Java 25 (Temurin), Swing, FlatLaf 3.6 + FlatLaf Extras (SVG icons), MigLayout 11.4.2, Maven
- PostgreSQL through JDBC 42.7.7
- Apache POI 5.4.1 (Excel) and Apache PDFBox 3.0.5 (PDF)
- `jpackage` through Maven for the Windows installer

## Architecture

Base package `com.cartonapp`:

- `model`: records (customer, product, invoice document/lines/totals, statement entries, reports data).
- `dao`: all SQL with `PreparedStatement`. `SchemaUpgrades` adds new columns/tables automatically (idempotent, additive) the first time they are needed, so existing databases upgrade without running `DatabaseSetup`.
- `service`: calculations and use cases — `InvoiceCalculator` (template formulas), `InvoiceValidationService`, invoice template renderer + Java2D/PDF painters, Excel template export, `ReportService`/`ReportExportService` (customer/product reports), `ExportFiles` (export names), `SettingsService` (company details, hashed password).
- `ui`: `MainFrame` screens, dialogs (`InvoiceEditorDialog`, `InvoicePreviewDialog`, `AppModalDialog`), shared components (`AppCard`, `MetricCard`, `SubTabBar`, `DatePickerField`, `SkeletonPanel`, `SplashWindow`, `TopNavigation`, `BrandLogo`), `Theme` (all colours) and `Animator`.
- `config`: `db.properties` loading, connection factory, `CompanyProfile`.

Calculated values (invoice totals, paid, remaining, balances) are never stored; they are computed from lines, discounts and receipts.

## Implemented

- Login with a stored, salted PBKDF2 password (default `123` until changed in Settings); animated login screen and splash.
- Customers: list with search/activity/debt-range filters, row click opens the profile, edit/delete (delete removes the customer and all activity after a confirmation with counts), opening debt field, profile with totals, statement (date pickers and quick periods, brought-forward line), products and invoices tabs, receipts (cash).
- Products: list, row click to the product page, manual carton price, specifications (كرافت/فلوت/تيست with numbers), product page with real price history, formula breakdown, sales and re-pricing.
- Invoices: official Excel template layout for create/edit/view/print/PDF/Excel; line discounts, additional discount, cash paid, previous balance (including opening debt); numbering `INV-00001`; delete cancels the invoice and its cash receipt.
- Reports: customer list, customer statement, product list, product card — PDF and Excel, named e.g. "فاتورة <العميل> <التاريخ>" / "كشف حساب <العميل> <التاريخ>" / "<المنتج> - <العميل>", in `exports/`.
- Settings: company details (used on invoices and reports), password change, pricing constants (read only), database status, exports folder.
- Design: navy / baby-blue / blue brand palette, logo, rounded cards, gradient buttons, field icons, animations (page fade, card entrance, count-up numbers, hover glow, skeleton loading, toast countdown).

## Verification approach

Each change was checked with scripted runs against a temporary copy of the database (created and dropped each time): invoice calculations and balances, reopen/edit/cancel, statement and report agreement, specifications, settings/password, real clicks inside `MainFrame` (off screen) and screenshots of the screens.

## Deployment

See `docs/DEPLOYMENT.md` (steps in Arabic). Summary: settings in `%APPDATA%\LeithiPack\` (`db.properties`), exports in `Documents\الليثي باك\التصدير\`, tables created automatically on first start (`DatabaseBootstrap`, no demo data). `AppPaths` holds these locations.

## Known gaps

- Excel files of invoices with more than ten lines were checked structurally but could not be opened in Excel by automation on this machine.
- Printing to a physical printer was not tested.
- Products are customer-specific; the invoice form only offers the selected customer's products (older invoice lines keep their product).
- The Git repository root is `C:\Users\DELL` with no commits; set up a proper repository for the project folder before committing.

## Commands

```powershell
# Fast verification (skips the exe step)
mvn clean package -Dexec.skip=true

# Run the application
mvn compile exec:java -Dexec.mainClass=com.cartonapp.Main

# Optional: create schema + seed data on an empty database
mvn compile exec:java -Dexec.mainClass=com.cartonapp.DatabaseSetup

# Full build with the Windows installer (only when needed; requires WiX 3)
mvn clean package
```

`db.properties` holds the local database settings and must not be committed.
