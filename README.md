# الليثي باك — Carton Accounting

تطبيق سطح مكتب لمحاسبة مصنع الكراتين المموجة، مكتوب بـ Java/Swing، واجهة عربية كاملة من اليمين لليسار.

> **Desktop accounting application** for a corrugated-carton factory. Manages customers, products with automatic carton pricing, sales invoices, receipts, and customer account statements. Single-user, Windows only.

---

## Features

- **Login** – PBKDF2-hashed password with animated splash screen (default: `123`)
- **Customers** – list with search, filters, opening debt; full profile with statement, invoices & receipts tabs
- **Products** – customer-specific carton products, manual pricing, formula breakdown, price history
- **Invoices** – official Excel template layout; line discounts, additional discount, cash paid, previous balance; numbered `INV-00001`; PDF and Excel export
- **Reports** – customer list, customer statement, product list, product card — PDF and Excel
- **Settings** – company details (printed on invoices), password change, database status, exports folder
- **Design** – navy/baby-blue brand palette, rounded cards, gradient buttons, animations, RTL throughout

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 (Temurin) |
| UI | Swing + FlatLaf 3.6 + FlatLaf Extras (SVG icons) |
| Layout | MigLayout 11.4.2 |
| Database | PostgreSQL via JDBC 42.7.7 |
| Excel | Apache POI 5.4.1 |
| PDF | Apache PDFBox 3.0.5 |
| Build | Maven + jpackage (Windows installer) |

---

## Project Structure

```
src/main/java/com/cartonapp/
├── config/      # DB connection, company profile, AppPaths
├── dao/         # All SQL (PreparedStatement only)
├── model/       # Plain data records
├── service/     # Business logic, pricing, invoice calculations, exports
└── ui/          # Frames, dialogs, reusable components, Theme
```

---

## Getting Started

### Prerequisites

- Java 25 (Temurin recommended)
- Maven 3.9+
- PostgreSQL 14+ running locally
- WiX Toolset 3 *(only for the Windows installer build)*

### Database Setup

1. Create a PostgreSQL database (e.g. `cartonapp`).
2. Copy `db.properties.example` to `db.properties` (or create it) with your connection details:

```properties
db.url=jdbc:postgresql://localhost:5432/cartonapp
db.user=postgres
db.password=your_password
```

> `db.properties` is **git-ignored** — never commit it.

The schema is created automatically on first launch via `DatabaseBootstrap`.  
To seed an empty database manually:

```powershell
mvn compile exec:java -Dexec.mainClass=com.cartonapp.DatabaseSetup
```

### Build & Run

```powershell
# Fast build (skips installer — use during development)
mvn clean package -Dexec.skip=true

# Run the application
mvn compile exec:java -Dexec.mainClass=com.cartonapp.Main

# Full build — creates the Windows .exe installer (requires WiX 3)
mvn clean package
```

The installer is output to `target/dist/`.

---

## Data & File Locations (Runtime)

| Purpose | Path |
|---|---|
| Settings & DB config | `%APPDATA%\LeithiPack\` |
| Exported files | `Documents\الليثي باك\التصدير\` |

---

## Architecture Notes

- **No business logic in UI** — `ui` calls `service`, `service` calls `dao`.
- **Calculated values are never stored** — invoice totals, paid amounts, remaining balances and customer balances are computed fresh from invoice lines and receipts every time.
- **BigDecimal everywhere** — no `double` or `float` for money.
- **EDT safety** — all database work runs in `SwingWorker`; UI starts in `SwingUtilities.invokeLater`.

---

## License

Private — internal use only.
