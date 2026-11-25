# AGENTS.md

Instructions for the coding agent. Read this file fully before doing any task.

## Project
Desktop application for a corrugated carton factory. Used by ONE user (the accountant) on ONE Windows PC.
The app manages: customers, products with automatic carton pricing, invoices, receipts, and customer account statements.
Out of scope: inventory and raw materials, production, purchasing and suppliers, general ledger, e-invoicing, multiple users and roles.

UI language: Arabic, full right-to-left. Money values always have exactly 2 decimals. Currency: Egyptian pound.

## Stack
- Java 25 (Temurin), Swing, FlatLaf look and feel
- Maven only (no manual jar files, no Gradle)
- PostgreSQL through the official JDBC driver
- Windows exe built with jpackage through Maven

## Package structure
Base package: com.yourcompany.myapp  (CHANGE to the real package)
- model: plain data classes
- dao: all SQL code, nothing else
- service: business logic and calculations (pricing, invoice totals, balances), calls dao
- ui: frames, panels, dialogs, reusable components, Theme class
- config: database settings loading
- Main: entry point

## Rules
- All SQL lives in dao classes only, always with PreparedStatement.
- No database or slow work on the Swing EDT. Use SwingWorker.
- Start the UI inside SwingUtilities.invokeLater.
- Never hardcode passwords or connection strings. Read them from db.properties (not committed to git).
- Add libraries only in pom.xml. Ask before adding a new one. Already approved: flatlaf, flatlaf-extras (SVG icons), miglayout-swing, PostgreSQL JDBC driver.
- Do not change the build setup in pom.xml (compiler, jar, dependency copy, jpackage steps) without asking.
- Layouts are written in code. No generated GUI builder code (no NetBeans .form files).
- Keep layers separate: ui calls service, service calls dao. No business logic inside ui.
- Calculated values (invoice total, paid, remaining, customer balance) are NOT stored. They are calculated every time from invoice lines and receipts.
- Use BigDecimal for all money. Never double or float.
- Do not invent business rules or formulas. Pricing and business rules are defined in the docs folder when present. If something is not defined, ask.

## Figma Make to Swing workflow
The screen design is a Figma Make prototype (a React app). It is available in two ways:
- Through the Figma MCP tools, using ONE link to the whole Make file (there are no per-screen links).
- As screenshots in docs/screens, numbered in screen order (01-login.png, 02-customers-list.png, and so on).

For every screen:
1. Read the Make project files through the Figma MCP link (get all the project files, mainly the App file and the CSS file) to find that screen's structure, texts, sizes, spacing, colors and fonts. If the MCP link does not work, read the same files from docs/figma-make-source if present.
2. The Make code is React and Tailwind. Use it only as a reference. Never write React, HTML or CSS. Implement in Java Swing.
3. Look at the matching screenshot in docs/screens and match it as closely as Swing allows. Where Swing cannot match exactly (rounded cards, badges, shadows), use FlatLaf client properties or small custom painting and keep it simple. If a screen has no screenshot, build it from the code and say so.
4. Set ComponentOrientation to RIGHT_TO_LEFT on the whole window and mirror layouts.
5. Put colors, fonts and spacing (taken from the Make CSS and code) in one Theme class. Do not hardcode them in screens.
6. Icons: the prototype uses icons by name. Create matching simple SVG files in src/main/resources/icons and load them with FlatSVGIcon.
7. Build reusable components once in the ui package (primary button, status badge, toast, modal dialog, table style, top tab bar) and reuse them.
8. Implement only the screens you were asked for. Do not touch other screens.

Until the database stage starts, screens get data from service interfaces that return sample data.

## Fidelity rules (important)
- A screen is "complete" only if every element visible in its screenshot and Make code exists: layout columns, side navigation, sub tabs, summary cards and metrics, filters, pagination, badges, buttons, icons, form sections, validation messages, footers, empty states.
- Never simplify silently. If something cannot be built in Swing, build the closest version and list it in docs/PROGRESS.md under "Differences".
- Before coding a screen, write its checklist of elements (from the screenshot and the Make code) in docs/PROGRESS.md. After coding, tick each item. Do not mark the screen complete with unticked items.
- docs/PROGRESS.md uses three statuses only: complete, simplified (with the missing items listed), not started.
- Be honest in reports. Do not describe a simplified screen as fully implemented.

## Commands
- During development, verify with the fast build that skips the exe step: mvn clean package -Dexec.skip=true
- Do NOT run the full build (it runs jpackage, takes minutes, wastes time and tokens) unless the user explicitly asks for the exe. If a jpackage run is already going and was not requested, stop waiting for it and continue.
- Full build with the exe (only when asked): mvn clean package
- Result of the full build: target/dist
- The user runs and reviews the app manually, so after the fast build passes, do not launch the GUI or wait for it. Just report how to run it.

## Definition of done for each screen
- mvn clean package -Dexec.skip=true passes with no errors.
- The screen opens from the app and looks like the screenshot.
- RTL and Arabic text display correctly.
- Report which parts do not match the design exactly.

## Notes
- The first build downloads libraries, so it needs internet.
- The exe installer needs WiX Toolset 3 on the build machine.
- The app is also tested in NetBeans (open the folder as a Maven project).
- Other specification files may exist in the docs folder (requirements, data model, pricing formula, screens, phases). Read the ones relevant to your task.