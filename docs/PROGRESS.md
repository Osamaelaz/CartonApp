# Shared foundation

Status: complete

- [x] Custom 34px title bar: brand mark, app name, version separator, and window-control visual treatment.
- [x] 57px top tab bar: RTL spacing, active underline, active/hover colors, and matching SVG icons.
- [x] SVG resource set for every icon named in the Make project: users, box, file, gear, search, plus, download, edit, print, back, calendar, lock, more, close, chevron, alert, chart.
- [x] Status badges: gray, green, blue, orange, red, with Make palette, compact padding, and rounded pill shape.
- [x] Shared table: 36px header, 52px rows, hover/selection state, sort indicator, filter bar, and 47px pagination bar.
- [x] Summary metric cards: blue, green, orange, neutral accent variants.
- [x] Shared sub-tabs and settings-style side navigation.
- [x] Modal dialog shell: icon header, native dialog close control, section panels, inline validation, and footer actions.
- [x] Shared toast, primary/secondary/danger/ghost buttons, text fields, combo boxes, and date field.
- [x] `ComponentsDemo` displays the complete shared foundation for manual review.
- [x] Global RTL propagation (labels, inputs, combos, dialogs, and workspaces), explicit Arabic top-tab ordering, reliably classpath-loaded colored SVG icons, full-width workspace layout, and interactive customer-detail sub-tabs.

## Differences

- Native Swing window minimise/maximise/close controls remain in the operating-system title bar; duplicate controls were removed from the main content title bar and modal header.
- Swing has no native CSS hover transition or browser-native date picker. The component uses FlatLaf hover states and a formatted date field with a calendar icon.

# Database interaction

Status: simplified

- [x] Customers: create, edit, view selected details, and guarded physical delete use PostgreSQL and refresh the list after completion.
- [x] Products: create, edit, view selected identity details, price update/history entry, and logical delete use PostgreSQL and refresh the list after completion.
- [x] Invoices: list, create, view, edit, delete (cancel), print, PDF and Excel export use PostgreSQL and the official sales-invoice template (see "Sales invoice template" below).
- [x] All database actions run with `SwingWorker`; the UI refreshes only after a successful completion.
- [x] All 17 packaged SVG resources parse as valid XML and are copied to the runtime classpath.

Differences: Logical delete is deliberately used for customers/products and invoice cancellation rather than a physical delete, preserving accounting history.

# 01 Login

Status: complete

- [x] RTL full-window canvas with centred white card.
- [x] Blue chart mark, Arabic title/subtitle, password label and lock icon.
- [x] Password placeholder, primary open button, and single-user local-program note.
- [x] Non-empty sample-password flow and visible validation message for an empty entry.
- [x] Make custom login title bar, window-control visuals, and title-bar drag behavior.
- [x] Empty-password validation message and red field border/error outline.

Differences: the supplied JPG and Make CSS give different card dimensions. The implementation follows the Make source's explicit 390px card width and spacing values.

# 02 Customers list

Status: complete

- [x] Page eyebrow, Arabic heading, count, Excel action, and new-customer action.
- [x] Search/filter area, debtor-only checkbox, database customer rows, account codes, and values.
- [x] Sixth customer row added to match the supplied screenshot.
- [x] Table header, rows, money alignment, badges, code colour, and page-number pagination controls.
- [x] Each row has compact color-coded `عرض` and `تعديل` icon buttons with tooltips; navigation is no longer triggered by clicking the row itself.
- [x] Make-matched `TopNavigation` component with active customer tab icon and underline.
- [x] One combined search field covers customer name, activity, and phone, plus the debtors-only filter.
- [x] Customer table displays 10 rows per page, with dynamic page numbers and RTL previous/next arrows in the table footer.
- [x] Customer serial numbers are centered in their cells, with increased row and header spacing.
- [x] Invoice list now uses the same centered table treatment, visible view/edit actions, real database rows, filters, and dynamic six-row pagination.
- [x] Shared `PaginationBar` centralizes RTL controls, six-row pages, dynamic page counts, and `عرض start–end من total` counters for customer, product, and invoice lists.
- [x] Customer-name secondary industry line, screen-specific column widths, sorting affordances, and hover treatment.
- [x] Pagination uses six rows per page, updates after filtering, and enables page navigation when more records exist.
- [x] Customer rows, balances, opening a selected customer, editing, saving, and Excel export now use PostgreSQL data.
- [x] Customer count is presented as a compact status badge, and validation space is reserved so errors do not move input fields.
- [x] Customer pagination arrows use directional SVG icons with RTL previous/next placement and tooltips.
- [x] Excel export creates a branded, styled workbook with real balances, numeric money formatting, filters, frozen headings, and totals.
- [x] PDF export creates a branded PDF with an Arabic-compatible Windows font, styled headings, real customer data, totals, and multi-page support.
- [x] Exported visible numbers and dates use Arabic-Indic digits, with RTL sheet/page direction and right-aligned Arabic columns.
- [x] Accounting exports include customer code, name, activity, phone, address, and balance.
- [x] Excel money cells remain numeric and sortable, using Arabic-Egyptian accounting number formats; identifiers and phone values remain text.
- [x] Customer statement printing sends the database-backed statement to the selected Windows printer with Arabic RTL formatting.

Differences: filtering and pagination are client-side over the loaded database result; server-side pagination is not yet implemented.

# 03 Customer page

Status: complete

- [x] Customer breadcrumb, name, industry, back affordance, and edit/print/new-invoice/delete actions.
- [x] Read-only customer information card: code, phone, and address.
- [x] Three summary metrics: invoice count, product count, and account-statement movement count.
- [x] Account/products/invoices sub-tabs including count pills.
- [x] Statement header, explanatory text, date range with calendar icon, and add-receipt action.
- [x] Statement print action loads the selected customer's real movements asynchronously and opens the native printer dialog.
- [x] Statement rows now load from posted invoices and active receipts in PostgreSQL, with service-calculated running balances and period totals.
- [x] Receipt dialog persists active customer receipts asynchronously and refreshes the selected customer's statement.
- [x] Full six-row statement table with debit, credit, running balance, period-total footer, and pagination bar.
- [x] Make top navigation with Customer icon and active underline.
- [x] Products/invoices sub-tabs and statement pagination controls rendered in the Customer page.
- [x] Selected-customer identity fields load from PostgreSQL; its Products tab asynchronously loads that customer's real product rows and opens their matching detail header.

Differences: invoice-tab rows and their count badges remain the supplied design data.


# 04 Products list

Status: complete

- [x] Page eyebrow, Arabic heading, product count, copy-product action, and new-product action.
- [x] Search/filter area with customer dropdown.
- [x] Product codes, product/customer/size/price columns, row navigation, and all five screenshot rows.
- [x] Table header, value alignment, and page-number pagination controls.
- [x] Make-matched `TopNavigation` component with active products tab icon and underline.
- [x] Search SVG inside the field; text search and customer dropdown filter the sample rows.
- [x] Product rows expose separate `عرض` and `تعديل` action buttons; clicking the row body no longer navigates.
- [x] Screen-specific column widths, sort markers, hover treatment, and detail affordance.
- [x] Pagination bar matches the specified single-page sample-data state.
- [x] Product catalogue rows, customer choices, filtering, and selected-product detail header now load from PostgreSQL.

Differences: the form, copy action, pricing breakdown and pagination are not yet persisted/derived from database data. The customer filter choices are derived from loaded product rows; loading all customers independently is still pending. Copy requires a product-code naming rule before it can safely create a record.

# 05 Product page

Status: complete

- [x] Product breadcrumb, title, customer subtitle, back control, edit action, and repricing action.
- [x] Right-side main column with carton specifications and all nine design fields.
- [x] Computed sheet-area and carton-weight highlight fields.
- [x] Paper-layers card with three rows, type badges, GSM, and per-kilogram prices.
- [x] Pricing-components card with four calculation rows and costs.
- [x] Left-side price-breakdown card, price header, update date, seven cost rows, and emphasized final price.
- [x] Price-history card with all three dated previous-to-current price transitions.
- [x] Make-matched product two-column proportions, card gaps, table dimensions, RTL orientation, and active Products navigation tab.

Differences: data comes from the required sample data until the database stage. The CSS gradient is approximated with Swing `GradientPaint`; it uses the source colours and direction but Swing anti-aliasing may differ slightly. The source assets are available in the repository root as `screens/05-product-page.jpg` and `figma-make-source/src/App.tsx`, rather than beneath `docs`.

# 08 Invoices list

Status: complete

- [x] Sales-and-collection eyebrow, Arabic heading/subtitle, and icon-bearing new-invoice action.
- [x] Three compact monthly summary cards for posted total, collected, and remaining values.
- [x] Filter bar with embedded search icon, all-status dropdown, and dynamic displayed-result count.
- [x] Eight-column RTL invoice table with sort markers, five screenshot rows, invoice codes, dates, customer names, money columns, and detail chevrons.
- [x] Full invoice-status badge treatment: posted, draft, cancelled; and payment-status badges: paid, partly paid, unpaid.
- [x] Table hover/selection treatment, screen-specific widths, and detail navigation affordance.
- [x] 47px pagination footer with total, disabled previous control, selected page, remaining pages, next control, and per-page count.
- [x] Active Invoices top-navigation tab and RTL layout.

Differences: the search and status filters operate on the required sample data. Page controls show the source's first-page state; database-backed paging and the invoice detail screen are deferred until their database/detail-screen stages. The source assets are available in the repository root as `screens/08-invoices-list.jpg` and `figma-make-source/src/App.tsx`, rather than beneath `docs`.

# 06 Customer modal form

Status: complete

- [x] Modal shell with right-to-left layout, customer/file icon, Arabic title, close control, border, and compact dimensions.
- [x] Two-column basic form fields: disabled automatic code, required name, activity, and phone placeholder.
- [x] Full-width address field.
- [x] Inline validation strip explaining required fields.
- [x] Tinted footer with cancel and primary save buttons, including working sample save feedback.
- [x] Form field borders, disabled state, labels, spacing, and footer/header separators matching the Make source and screenshot.

Differences: the modal does not yet expose duplicate-code or server-side field validation; successful saves persist through the customer service and refresh the database-backed list. The source assets are available in the repository root as `screens/6-modal-customer.jpg` and `figma-make-source/src/App.tsx`, rather than beneath `docs`.

# 07 Product modal form

Status: complete

- [x] Wide RTL modal shell with product/file icon, Arabic title, close control, and header/footer separators.
- [x] Structured basic-data section with a database-backed customer selector, required name, and carton type.
- [x] Structured dimensions/specifications section with four-column first row and three-field second row, including centimetre suffixes.
- [x] Paper-layers section with add-layer icon action and inline empty-state explanation.
- [x] Required-field validation feedback for the customer and name fields.
- [x] Tinted footer with cancel and primary save buttons, including working sample save feedback.
- [x] Source-matched section borders, headings, field spacing, RTL alignment, and compact input styling.

Differences: paper-layer selection is intentionally removed; the price is derived from dimensions and the configured base pricing constants. The source assets are available in the repository root as `screens/07-modal-product.jpg` and `figma-make-source/src/App.tsx`, rather than beneath `docs`.


# Sales invoice template (فاتورة_مبيعات_تجريبيه.xlsx)

Status: complete

Source: the Excel workbook supplied by the user, bundled as `src/main/resources/templates/sales-invoice.xlsx`
(calcChain and the author's file path removed; cells, styles, merges and formulas unchanged).

Document (preview, print, PDF; one layout engine `InvoiceTemplateRenderer`, drawn by `InvoiceJava2DPainter` and `InvoicePdfService`):
- [x] Rows 1-2: company band (الليثي باك, address | phone) on the template's blue fill, configurable through optional `company.properties`.
- [x] Row 4: orange "فاتورة مبيعات" title, red invoice number, gray "تاريخ البيان" label and yyyy/MM/dd date.
- [x] Rows 6-8: العميل, العنوان, اسم الشركة from the invoice's customer record (gray labels, merged B:D values).
- [x] Row 10: navy seven-column header مسلسل, الصنف, الكمية, الوحدة, القيمة, قيمة الخصم, الصافي.
- [x] Rows 11-20: at least ten numbered rows; more lines continue on more pages; long item names wrap and grow the row.
- [x] Rows 22-25: المدفوع نقداً, إجمالي الكميات, قيمة الفاتورة, الرصيد النهائي, الرصيد السابق, خصم إضافي, إجمالي الفاتورة والمديونية.
- [x] Rows 25-26: red acknowledgement text; row 28: المستلم, مسؤول المبيعات, الختم.
- [x] A4 portrait, template column proportions, Arial (the workbook's Arabic theme font), template colours and number formats.
- [x] Multi-page: company band, title row and table header repeat; totals and signatures stay together; "صفحة n من m" footer.
- [x] PDF: vector text with embedded Arial, Arabic shaping (incl. lam-alef and hamza forms), RTL bidi ordering, mirrored brackets.
- [x] Print: real `PrinterJob` document (not a screenshot), A4, scaled into the printer's printable area when needed.
- [x] Excel export: fills a copy of the template; extra rows for more than ten lines; formulas kept and values pre-computed.

Calculations (`InvoiceCalculator`, BigDecimal, 2 decimals half-up, identical to the SQL):
- [x] القيمة = الكمية × الوحدة; الصافي = القيمة − قيمة الخصم; قيمة الفاتورة = Σ الصافي; إجمالي الكميات = Σ الكمية.
- [x] إجمالي الفاتورة والمديونية = قيمة الفاتورة + الرصيد السابق − خصم إضافي; الرصيد النهائي = ذلك − المدفوع نقداً.
- [x] الرصيد السابق is calculated from the customer's earlier posted invoices and receipts (not the current balance).

Operations:
- [x] Create / edit form with real customers and products, line discounts, additional discount, cash paid, live totals and previous balance.
- [x] Arabic validation (customer, date, at least one line, product, quantity > 0, price >= 0, discount <= line value, additional discount <= invoice value, cash >= 0, max two decimals) in the form and again in the service.
- [x] View (عرض) opens the preview of the final document with print, PDF, Excel, edit and delete actions; "حفظ ومعاينة" opens it after saving.
- [x] Edit keeps the invoice number; lines, discounts and cash receipt are updated in one transaction.
- [x] Delete keeps the confirmation, cancels the invoice and its cash receipt in one transaction, and hides it from the lists.

Verification: a scripted run against a temporary copy of the database (old schema, upgraded automatically) checked creation,
reopening in a new JVM, editing, cash receipt changes, same-day previous balance, 27- and 70-line invoices, cancellation,
balances and statement agreement; PDF and Java2D outputs were compared with Excel's rendering of the template.

Differences:
- The template's "الوحدة" column holds the unit price (its formula is القيمة = الكمية × الوحدة); the document keeps that label. The form labels the field "الوحدة (السعر)".
- "اسم الشركة" shows the customer name: customer records have no separate company-name field.
- The document is scaled uniformly to fit the seven template columns on A4 width (Excel itself prints the template on two pages).
- Excel's quantity-total cell keeps the template's #,##0 format unless quantities have fractions; the invoice-number cell shrinks to fit instead of wrapping.
- The exported workbook uses IF(ISNUMBER(F),F,0) instead of N(F) in الصافي, with the same result, so POI can store computed values.
- Customer details printed on an invoice come from the current customer record; only item names are snapshotted per line.
- Inventory is out of scope for this application, so no stock is changed.

# Customer opening debt, account totals and exports

Status: complete

- [x] Customer form: "المديونية السابقة (رصيد افتتاحي)" field (optional, two decimals, Arabic digits accepted), editable later.
- [x] Opening debt is part of the customer balance, every invoice's الرصيد السابق and the first statement row.
- [x] Customer page: إجمالي الفواتير, المحصّل and المديونية cards plus the opening debt in the customer details.
- [x] Statement for a period starts with "رصيد سابق مُرحّل", so its closing balance equals the customer's debt; same-day invoices are listed before their payments.
- [x] Excel and PDF export for the customer list, each customer (account statement), the product list and each product (specification and sales), in the exports folder, using the invoice template's visual style.
- [x] Customer table delete column: deletes the customer with all invoices, receipts and products after a confirmation that lists the counts.
- [x] Money on screens always shows two decimals; action buttons widen to fit their labels.

Verification: scripted checks on a temporary database confirmed the customer list, customer page totals, invoice previous balance, statement closing balance and all four reports show the same numbers, including after editing the opening debt.

# Settings

Status: complete

- [x] Company details (name, address, phone) saved in `app_settings` and used by invoices and PDF/Excel reports.
- [x] Password change: current password check, minimum 4 characters, confirmation match, must differ from the current one; Arabic messages.
- [x] Login uses the saved password (salted PBKDF2-SHA256 hash). Until the first change the original password `123` keeps working and a warning is shown.
- [x] Pricing formula card shows the real constants used by `ProductPricingService` (read only).
- [x] System card: database, user, connection status with a test button, exports folder with an open button, version.
- [x] Removed the old placeholder values (7% waste, 14% tax) that were not used anywhere.

# Brand redesign (navy / baby blue)

Status: complete

- [x] Theme palette: navy structure, baby-blue surfaces and highlights, blue actions, green/orange/red status; all tokens in `Theme`.
- [x] Logo: `icons/logo.svg` (carton box on a navy-to-blue tile) with the factory name "الليثي باك" via `BrandLogo`; used for window/taskbar icons, top bar and login.
- [x] Navy top bar with brand, pill tabs (baby-blue active underline), settings and user chip; navy window title bar.
- [x] Login screen: navy brand panel (logo, name, tagline, feature list) beside the login card.
- [x] Key-figure cards with tinted icon badges and accent strip; new icons: receipt, wallet, coins, chart, tag, trending, layers, shield, user-circle.
- [x] Tables: baby-blue headers with navy text, bluish grid, baby-blue selection/hover; pagination, sub-tabs, toast and buttons restyled.

Differences: this replaces the original Figma Make palette (white/gray with #1769C2) at the user's request. The sales-invoice
document keeps the colours of the official Excel template.
- [x] Plain white replaced: blue-gray canvas (#E4EDF7), soft blue surface for cards/dialogs/tables (#F6F9FE), zebra table rows; input fields stay white so they read as editable.
- [x] Table actions redesigned as soft buttons (tinted background, coloured outline icon: eye / pencil / trash; solid colour while pressed) on customers, products and invoices.
- [x] No plain white surfaces: gradient page canvas, tinted cards (#EFF5FD), baby-blue card/section header bands with a blue accent, navy table headers, navy gradient dialog headers, tinted dialog footers.
- [x] Field icons chosen automatically from the label (`FieldIcons`): name, phone, address, activity, dates, amounts, prices, dimensions, layers, colours, password.
- [x] Animations (`Animator`): page fade-in, key figures count up, toast slides in, smooth tab hover.
- [x] View/edit/delete icons are the same everywhere, including the customer page's products and invoices tabs (eye opens the product / invoice).

# Customers list: row click and filters

Status: complete

- [x] Clicking anywhere on a customer row opens the customer profile (hand cursor on rows); edit and delete buttons keep their actions.
- [x] Filter bar: search (name, code, address, phone, activity), activity, debt range from/to, "مسح الفلاتر" and an active-filter count (balance-status and sort filters removed at the user's request).

# View column removed, livelier visuals

Status: complete

- [x] No "عرض" column anywhere: clicking a row opens the customer profile, the product page or the invoice document (customers, products, invoices and the customer page tabs). Edit and delete remain as buttons.
- [x] Edit/delete buttons: gradient pills (blue / red) with white icons and a soft coloured shadow; brighter on hover, deeper when pressed.
- [x] Primary buttons with a blue-to-navy gradient and shadow; gradient top bar with transparent tabs; gradient icon badge beside every page title; gradient badges on key-figure cards.

# Customer page polish, rounded cards

Status: complete

- [x] Statement period: `DatePickerField` (calendar icon, pop-up month calendar with Arabic month/day names, "اليوم", red outline for an invalid date) plus quick periods: هذا الشهر، آخر 3 شهور، هذه السنة، كل الفترات; picking a date refreshes the statement.
- [x] Customer page tabs (كشف الحساب / منتجات العميل / فواتير العميل) are pill buttons with icons and count badges; the active tab is a blue gradient.
- [x] Customer details with gradient round icons (code, phone, address, opening balance); statement title badge; wallet icon on "إضافة تحصيل".
- [x] All cards (and the tables inside them) have rounded corners with a soft shadow; scroll panes have no square inner border.
- [x] Every date input uses `DatePickerField` (customer statement from/to, invoice form "تاريخ البيان", receipt form); the old spinner date pickers are removed.
- [x] Product page "مواصفات الكرتونة": coloured detail tiles with icons (code, customer, box type, dimensions, layers, colours) and a separate "محسوب تلقائيًا من المقاس" group for sheet length, width and area.

# Product specifications and contrast

Status: complete

- [x] Product form section "المواصفات" with checkboxes كرافت، فلوت، تيست (several allowed), placed under the basic data; saved in `products.paper_specs` (added automatically) and kept when re-pricing.
- [x] Product page shows the ticked specifications as coloured chips (كرافت brown, فلوت blue, تيست green).
- [x] Removed the placeholder "طبقات الورق" section from the product form (it only said the feature would come later).
- [x] Stronger contrast: darker muted/label/note text and borders, more saturated detail tiles; table text 13pt with bold navy names and bold blue amounts in every table.
- [x] Each specification has a number box beside its checkbox (enabled when ticked, 1-99); stored as "كرافت:2,فلوت:2" (older entries without a number count as 1) and shown on the product page as "2 كرافت في 2 فلوت".

# Export names and readable grey text

Status: complete

- [x] Export file names (`ExportFiles`): invoice "فاتورة <العميل> <تاريخ الإصدار>" (PDF and Excel, invoice date), customer statement "كشف حساب <العميل> <التاريخ>", product "<المنتج> - <العميل>", lists "العملاء <التاريخ>" / "المنتجات <التاريخ>"; existing files are never overwritten (" (2)" is added).
- [x] Print jobs and the invoice preview window use the same names, so "Print to PDF" suggests them as the file name; the statement report title is "كشف حساب <العميل>".
- [x] Grey text darkened everywhere (muted, note, label, placeholder, disabled text, units) and scattered hard-coded greys replaced by theme colours; small grey labels on the product page are larger and bold.

# Visual experience

Status: complete

- [x] Animated splash after login (logo grows in, name fades in, progress bar) and an animated login brand panel (drifting light, floating logo).
- [x] Cards slide up and fade in one after another when a page opens; key-figure cards glow and lift on hover.
- [x] Shimmering skeleton placeholders while customers, products, invoices and detail pages load.
- [x] Toast with a countdown bar, selected top tab fades in, active page number uses the gradient button.

# Deployment readiness

Status: simplified (installer built; not yet installed on a clean PC)

- [x] Settings and exports use per-user folders (`AppPaths`) instead of the read-only program folder; first-run message tells where `db.properties` goes or that PostgreSQL is unreachable.
- [x] Tables are created automatically on every start (`DatabaseBootstrap`, idempotent, no demo data); verified on an empty database.
- [x] `docs/DEPLOYMENT.md`: install steps, backup and update notes.
- [x] Installer built with `mvn clean package`: `target\dist\Carton Accounting-1.0.0.exe` (about 52 MB), logo icon (`packaging/app-icon.ico`), vendor "Leithi Pack", fixed upgrade id, bundled Java runtime with the required modules.
- [ ] Install and run it on a clean PC (first run, login, export) before handing it to the customer.
