package com.cartonapp.ui;

import com.cartonapp.config.DatabaseConfig;
import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.dao.CustomerDao;
import com.cartonapp.dao.ProductDao;
import com.cartonapp.dao.InvoiceDao;
import com.cartonapp.model.Customer;
import com.cartonapp.model.ProductSummary;
import com.cartonapp.model.InvoiceSummary;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.CustomerActivityCounts;
import com.cartonapp.model.StatementEntry;
import com.cartonapp.service.CustomerService;
import com.cartonapp.service.DatabaseCustomerService;
import com.cartonapp.service.ReportDocument;
import com.cartonapp.service.ReportExportService;
import com.cartonapp.service.ReportService;
import com.cartonapp.model.CustomerAccount;
import com.cartonapp.service.DatabaseProductService;
import com.cartonapp.service.ProductService;
import com.cartonapp.service.InvoiceService;
import com.cartonapp.service.DatabaseInvoiceService;
import com.cartonapp.service.CustomerStatementPrintService;
import com.cartonapp.service.InvoiceExportService;
import com.cartonapp.service.InvoicePrintService;
import com.cartonapp.service.InvoicePdfService;
import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.InvoiceDocument;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/** RTL workspace containing the supplied reference screens with sample data. */
public final class MainFrame extends JFrame {
    private final JPanel page = new JPanel(new BorderLayout());
    private final Toast toast = new Toast();
    private final TopNavigation topNavigation = new TopNavigation(this::navigateTop);
    private final CustomerService customerService;
    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final CustomerStatementPrintService statementPrintService = new CustomerStatementPrintService();
    private final InvoiceExportService invoiceExportService = new InvoiceExportService();
    private final InvoicePrintService invoicePrintService = new InvoicePrintService();
    private final InvoicePdfService invoicePdfService = new InvoicePdfService();
    /** Company details from the settings table; replaced when saved in the settings screen. */
    private volatile CompanyProfile companyProfile = CompanyProfile.loadDefault();
    private volatile ReportExportService reportExportService = new ReportExportService(companyProfile);
    private final com.cartonapp.service.SettingsService settingsService = openSettingsService();
    private String selectedCustomerCode = "C-001";
    private ProductSummary selectedProduct;
    private static final Color BACKGROUND = Theme.CANVAS;

    private record CustomerView(Customer customer, CustomerActivityCounts counts, CustomerAccount account) {
    }

    private record CustomerPrintData(String name, java.util.List<StatementEntry> entries) {
    }

    public MainFrame() {
        this(openCustomerService(), openProductService(), openInvoiceService());
    }

    private MainFrame(CustomerService customerService, ProductService productService, InvoiceService invoiceService) {
        super("الليثي باك");
        this.customerService = customerService;
        this.productService = productService;
        this.invoiceService = invoiceService;
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setIconImages(BrandLogo.windowIcons());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1366, 820);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        root.setBackground(BACKGROUND);
        page.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        root.add(topNavigation, BorderLayout.NORTH);
        root.add(page);
        root.add(toast, BorderLayout.SOUTH);
        setContentPane(root);
        loadCompanyProfile();
        customers();
    }

    private static CustomerService openCustomerService() {
        try {
            return new DatabaseCustomerService(
                    new CustomerDao(new DatabaseConnectionFactory(DatabaseConfig.loadDefault())));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ProductService openProductService() {
        try {
            return new DatabaseProductService(
                    new ProductDao(new DatabaseConnectionFactory(DatabaseConfig.loadDefault())));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static com.cartonapp.service.SettingsService openSettingsService() {
        try {
            return new com.cartonapp.service.SettingsService(new com.cartonapp.dao.SettingsDao(
                    new DatabaseConnectionFactory(DatabaseConfig.loadDefault())));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void useCompany(CompanyProfile company) {
        companyProfile = company;
        reportExportService = new ReportExportService(company);
    }

    /** Loads the saved company details off the Swing thread when the window opens. */
    private void loadCompanyProfile() {
        if (settingsService == null)
            return;
        new SwingWorker<CompanyProfile, Void>() {
            protected CompanyProfile doInBackground() throws Exception {
                return settingsService.company();
            }

            protected void done() {
                try {
                    useCompany(get());
                } catch (Exception ignored) {
                    // keep the template's company details
                }
            }
        }.execute();
    }

    private static InvoiceService openInvoiceService() {
        try {
            return new DatabaseInvoiceService(
                    new InvoiceDao(new DatabaseConnectionFactory(DatabaseConfig.loadDefault())));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void navigateTop(String destination) {
        switch (destination) {
            case "products" -> products();
            case "invoices" -> invoices();
            case "settings" -> settings();
            default -> customers();
        }
    }

    private JComponent navigation() {
        JPanel all = new JPanel(new BorderLayout());
        all.setBackground(Theme.CARD);
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 6));
        brand.setBackground(Theme.CARD);
        JLabel mark = label("▥", 18, Font.BOLD, Color.WHITE);
        mark.setOpaque(true);
        mark.setBackground(Theme.PRIMARY);
        mark.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        brand.add(mark);
        brand.add(label("الليثي باك", 13, Font.BOLD, Theme.NAVY));
        brand.add(label("الإصدار 1.0", 11, Font.PLAIN, Theme.MUTED_TEXT));
        all.add(brand, BorderLayout.NORTH);
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(Theme.CARD);
        nav.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setBackground(Theme.CARD);
        right.add(navButton("العملاء", this::customers));
        right.add(navButton("المنتجات", this::products));
        right.add(navButton("الفواتير", this::invoices));
        nav.add(right, BorderLayout.EAST);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        left.setBackground(Theme.CARD);
        left.add(navButton("الإعدادات", this::settings));
        nav.add(left, BorderLayout.WEST);
        all.add(nav, BorderLayout.SOUTH);
        return all;
    }

    private JButton navButton(String s, Runnable r) {
        JButton b = new JButton(s);
        b.setFont(Theme.font(Font.BOLD, 14));
        b.setForeground(Theme.MUTED_TEXT);
        b.setBackground(Theme.CARD);
        b.setBorder(BorderFactory.createEmptyBorder(17, 26, 17, 26));
        b.setFocusPainted(false);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void customers() {
        topNavigation.select("customers");
        if (customerService == null) {
            showCustomers(java.util.List.of());
            return;
        }
        JPanel loading = screen("إدارة الحسابات", "العملاء", "جارٍ تحميل بيانات العملاء…");
        loading.add(new SkeletonPanel(), "growx");
        setPage(loading);
        new SwingWorker<java.util.List<com.cartonapp.model.CustomerSummary>, Void>() {
            protected java.util.List<com.cartonapp.model.CustomerSummary> doInBackground() throws Exception {
                return customerService.findAllWithBalances();
            }

            protected void done() {
                try {
                    showCustomers(get());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل العملاء من قاعدة البيانات");
                    showCustomers(java.util.List.of());
                }
            }
        }.execute();
    }

    /** Which screen is showing, so an edit returns to the same place after saving. */
    private String currentScreen = "customers";

    private void showCustomers(java.util.List<com.cartonapp.model.CustomerSummary> rows) {
        currentScreen = "customers";
        JPanel p = screen("إدارة الحسابات / العملاء", "العملاء", customerCountSubtitle(rows.size()),
                action("تصدير Excel", AppButton.Kind.SECONDARY, () -> exportReport(false, ReportService::customerList)),
                action("تصدير PDF", AppButton.Kind.SECONDARY, () -> exportReport(true, ReportService::customerList)),
                action("+ عميل جديد", AppButton.Kind.PRIMARY, this::customerDialog));
        AppCard card = new AppCard();
        // Filter bar: search, balance status, activity, debt range and sort order.
        JPanel bar = new JPanel(new MigLayout("rtl,fillx,insets 12 14 10 14,gap 10 8",
                "[grow,fill][190!,fill][130!,fill][130!,fill][]"));
        bar.setBackground(Theme.SECTION);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        JTextField search = searchField("بحث بالاسم أو الكود أو العنوان أو الهاتف");
        search.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        java.util.List<String> industries = new java.util.ArrayList<>();
        industries.add("كل الأنشطة");
        rows.stream().map(r -> r.customer().industry()).filter(v -> v != null && !v.isBlank()).distinct().sorted()
                .forEach(industries::add);
        JComboBox<String> industry = FormField.combo(industries.toArray(String[]::new));
        JTextField minDebt = new JTextField();
        minDebt.putClientProperty("JTextField.placeholderText", "مديونية من");
        JTextField maxDebt = new JTextField();
        maxDebt.putClientProperty("JTextField.placeholderText", "إلى");
        for (JTextField f : java.util.List.of(minDebt, maxDebt)) {
            f.setHorizontalAlignment(SwingConstants.CENTER);
            f.setFont(Theme.font(Font.PLAIN, 12));
            f.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
            f.putClientProperty("JTextField.leadingIcon", Icons.icon("coins", 15, Theme.PRIMARY));
        }
        JButton clear = new AppButton("مسح الفلاتر", AppButton.Kind.GHOST, "close");
        clear = InvoiceEditorDialog.fitText(clear);
        bar.add(search);
        bar.add(industry);
        bar.add(minDebt);
        bar.add(maxDebt);
        bar.add(clear, "wrap");
        JLabel count = label("عرض " + rows.size() + " من " + rows.size(), 11, Font.BOLD, Theme.NAVY);
        JLabel activeFilters = label("", 11, Font.PLAIN, Theme.PRIMARY);
        JLabel hint = label("اضغط على أي عميل لفتح ملفه", 11, Font.PLAIN, Theme.MUTED_TEXT);
        hint.setIcon(Icons.icon("user", 14, Theme.MUTED_TEXT));
        hint.setIconTextGap(6);
        JPanel status2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        status2.setOpaque(false);
        status2.add(count);
        status2.add(activeFilters);
        status2.add(hint);
        bar.add(status2, "span,growx");
        card.add(bar, BorderLayout.NORTH);

        Object[][] data = new Object[0][7];
        JTable t = table(new String[] { "المسلسل", "الاسم", "العنوان", "الهاتف", "المديونية", "تعديل", "حذف" },
                data);
        setColumnWidths(t, 75, 245, 200, 150, 150, 58, 58);
        t.setRowHeight(48);
        t.getTableHeader().setPreferredSize(new Dimension(1, 40));
        DefaultTableCellRenderer serialRenderer = new DefaultTableCellRenderer();
        serialRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        serialRenderer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        t.getColumnModel().getColumn(0).setCellRenderer(serialRenderer);
        t.getColumnModel().getColumn(5)
                .setCellRenderer(new TableActionRenderer("تعديل", "edit", Theme.PRIMARY));
        t.getColumnModel().getColumn(6)
                .setCellRenderer(new TableActionRenderer("حذف العميل وعملياته", "delete", Theme.DANGER));
        java.util.List<com.cartonapp.model.CustomerSummary> filteredRows = new java.util.ArrayList<>();
        int[] currentPage = { 0 };
        int pageSize = PaginationBar.DEFAULT_PAGE_SIZE;
        Runnable[] render = { null };
        PaginationBar pagination = new PaginationBar();
        render[0] = () -> {
            filteredRows.clear();
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            BigDecimal min = filterAmount(minDebt);
            BigDecimal max = filterAmount(maxDebt);
            for (com.cartonapp.model.CustomerSummary row : rows) {
                Customer customer = row.customer();
                String searchable = (customer.code() + " " + customer.name() + " " + customer.address() + " "
                        + customer.phone() + " " + customer.industry()).toLowerCase(Locale.ROOT);
                boolean matchesSearch = query.isEmpty() || searchable.contains(query);
                boolean matchesIndustry = industry.getSelectedIndex() == 0
                        || industry.getSelectedItem().equals(customer.industry());
                boolean matchesMin = min == null || row.balance().compareTo(min) >= 0;
                boolean matchesMax = max == null || row.balance().compareTo(max) <= 0;
                if (matchesSearch && matchesIndustry && matchesMin && matchesMax)
                    filteredRows.add(row);
            }
            int active = (query.isEmpty() ? 0 : 1) + (industry.getSelectedIndex() == 0 ? 0 : 1)
                    + (min == null ? 0 : 1) + (max == null ? 0 : 1);
            activeFilters.setText(active == 0 ? "" : "• " + active + (active == 1 ? " فلتر نشط" : " فلاتر نشطة"));
            minDebt.putClientProperty("JComponent.outline", minDebt.getText().isBlank() || min != null ? null : "error");
            maxDebt.putClientProperty("JComponent.outline", maxDebt.getText().isBlank() || max != null ? null : "error");
            int pageCount = Math.max(1, (filteredRows.size() + pageSize - 1) / pageSize);
            currentPage[0] = Math.min(currentPage[0], pageCount - 1);
            int start = currentPage[0] * pageSize;
            int end = Math.min(start + pageSize, filteredRows.size());
            DefaultTableModel model = (DefaultTableModel) t.getModel();
            model.setRowCount(0);
            for (int i = start; i < end; i++) {
                com.cartonapp.model.CustomerSummary row = filteredRows.get(i);
                Customer customer = row.customer();
                model.addRow(new Object[] { i + 1, customer.name(), customer.address(), customer.phone(),
                        money(row.balance().toPlainString()), "تعديل", "حذف" });
            }
            count.setText(recordRange(filteredRows.size(), currentPage[0]));
            pagination.update(filteredRows.size(), currentPage[0]);
        };
        pagination.setPageListener(page -> {
            currentPage[0] = page;
            render[0].run();
        });
        t.putClientProperty("rowClickable", Boolean.TRUE);
        onCellClick(t, (viewRow, column) -> {
            if (viewRow >= t.getRowCount())
                return;
            int modelRow = t.convertRowIndexToModel(viewRow);
            int index = currentPage[0] * pageSize + modelRow;
            if (index >= filteredRows.size())
                return;
            selectedCustomerCode = filteredRows.get(index).customer().code();
            // Clicking anywhere on the row opens the customer's profile; the other buttons keep their actions.
            if (column <= 4)
                customer();
            else if (column == 5)
                editCustomerDialog();
            else if (column == 6)
                deleteCustomer(filteredRows.get(index).customer());
        });
        DocumentListener filters = change(() -> {
            currentPage[0] = 0;
            render[0].run();
        });
        search.getDocument().addDocumentListener(filters);
        minDebt.getDocument().addDocumentListener(filters);
        maxDebt.getDocument().addDocumentListener(filters);
        java.awt.event.ActionListener refilter = e -> {
            currentPage[0] = 0;
            render[0].run();
        };
        industry.addActionListener(refilter);
        clear.addActionListener(e -> {
            search.setText("");
            minDebt.setText("");
            maxDebt.setText("");
            industry.setSelectedIndex(0);
        });
        JScrollPane tableScroll = new JScrollPane(t);
        tableScroll.setPreferredSize(new Dimension(0,
                t.getTableHeader().getPreferredSize().height
                        + (t.getRowHeight() * PaginationBar.DEFAULT_PAGE_SIZE) + 2));
        card.add(tableScroll);
        card.add(pagination, BorderLayout.SOUTH);
        p.add(card, "grow,push");
        setPage(p);
        render[0].run();
    }

    /** Amount typed in a filter box (Arabic digits and separators accepted), or null when empty/invalid. */
    private static BigDecimal filterAmount(JTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty())
            return null;
        try {
            return parseOpeningBalance(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Builds a report from the services and writes it to the exports folder as PDF or Excel. */
    private void exportReport(boolean pdf, ReportBuilder builder) {
        if (customerService == null || productService == null || invoiceService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        toast.showMessage(pdf ? "جارٍ تصدير PDF…" : "جارٍ تصدير Excel…");
        ReportService reports = new ReportService(customerService, productService, invoiceService);
        new SwingWorker<java.nio.file.Path, Void>() {
            protected java.nio.file.Path doInBackground() throws Exception {
                ReportDocument report = builder.build(reports);
                return pdf ? reportExportService.exportPdf(report) : reportExportService.exportExcel(report);
            }

            protected void done() {
                try {
                    toast.showMessage((pdf ? "تم تصدير PDF: " : "تم تصدير Excel: ") + get().toAbsolutePath());
                } catch (Exception exception) {
                    exception.printStackTrace();
                    toast.showMessage((pdf ? "تعذر تصدير PDF: " : "تعذر تصدير Excel: ") + rootMessage(exception));
                }
            }
        }.execute();
    }

    @FunctionalInterface
    private interface ReportBuilder {
        ReportDocument build(ReportService reports) throws Exception;
    }

    private void customer() {
        if (customerService == null) {
            showCustomer(null, new CustomerActivityCounts(0, 0, 0),
                    new CustomerAccount(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            return;
        }
        JPanel loading = screen("العملاء", "جارٍ تحميل بيانات العميل…", "");
        loading.add(new SkeletonPanel(), "growx");
        setPage(loading);
        new SwingWorker<CustomerView, Void>() {
            @Override
            protected CustomerView doInBackground() throws Exception {
                return new CustomerView(customerService.findByCode(selectedCustomerCode).orElseThrow(),
                        customerService.findActivityCounts(selectedCustomerCode),
                        customerService.findAccount(selectedCustomerCode));
            }

            @Override
            protected void done() {
                try {
                    CustomerView view = get();
                    showCustomer(view.customer(), view.counts(), view.account());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل بيانات العميل");
                    customers();
                }
            }
        }.execute();
    }

    private void showCustomer(Customer current, CustomerActivityCounts counts, CustomerAccount account) {
        currentScreen = "customer";
        topNavigation.select("customers");
        String code = current == null ? "C-001" : current.code();
        String name = current == null ? "الصفوة للأجهزة المنزلية" : current.name();
        String industry = current == null ? "أجهزة منزلية" : current.industry();
        JPanel subtitle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        subtitle.setOpaque(false);
        subtitle.add(label(industry, 12, Font.PLAIN, Theme.MUTED_TEXT));
        JPanel p = screen("العملاء / " + code, name, subtitle,
                iconAction("فاتورة جديدة", AppButton.Kind.PRIMARY, "plus", this::invoiceForSelectedCustomer),
                iconAction("طباعة كشف حساب", AppButton.Kind.SECONDARY, "print", this::printCustomerStatement),
                iconAction("تصدير PDF", AppButton.Kind.SECONDARY, "download",
                        () -> exportReport(true, reports -> reports.customerAccount(code))),
                iconAction("تصدير Excel", AppButton.Kind.SECONDARY, "download",
                        () -> exportReport(false, reports -> reports.customerAccount(code))),
                iconAction("تعديل", AppButton.Kind.SECONDARY, "edit", this::editCustomerDialog),
                iconAction("عودة", AppButton.Kind.SECONDARY, "back", this::customers));
        p.add(customerInformation(current, account), "growx,wrap");

        JPanel metrics = new JPanel(new GridLayout(1, 3, 11, 0));
        metrics.setOpaque(false);
        // المديونية = الرصيد الافتتاحي + إجمالي الفواتير − المحصّل (calculated, never stored).
        metrics.add(new MetricCard("إجمالي الفواتير", money(account.invoicesTotal().toPlainString()),
                MetricCard.Tone.BLUE));
        metrics.add(new MetricCard("المحصّل", money(account.collected().toPlainString()), MetricCard.Tone.GREEN));
        metrics.add(new MetricCard("المديونية", money(account.balance().toPlainString()), MetricCard.Tone.ORANGE));
        p.add(metrics, "growx,wrap");

        CardLayout customerTabsLayout = new CardLayout();
        JPanel customerTabContent = new JPanel(customerTabsLayout);
        customerTabContent.setOpaque(false);
        customerTabContent.add(customerStatement(), "statement");
        customerTabContent.add(customerProductsTab(), "products");
        customerTabContent.add(customerInvoicesTab(), "invoices");
        SubTabBar subtabs = new SubTabBar(id -> {
            customerTabsLayout.show(customerTabContent, id);
        });
        // Reverse insertion retains the Arabic reading order on the right edge with
        // FlowLayout.
        // FlowLayout in RTL puts the first added item at the right edge.
        subtabs.addTab("statement", "كشف الحساب", String.valueOf(counts.statementEntries()));
        subtabs.addTab("products", "منتجات العميل", String.valueOf(counts.products()));
        subtabs.addTab("invoices", "فواتير العميل", String.valueOf(counts.invoices()));
        subtabs.select("statement");
        p.add(subtabs, "growx,wrap,gaptop 4");
        p.add(customerTabContent, "grow,push");
        setPage(p);
    }

    private JPanel customerInformation(Customer current, CustomerAccount account) {
        String code = current == null ? "C-001" : current.code();
        String phone = current == null ? "0100 234 8671" : current.phone();
        String address = current == null ? "المنطقة الصناعية الثالثة، مدينة 6 أكتوبر" : current.address();
        AppCard card = new AppCard();
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setBackground(Theme.CARD);
        grid.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        grid.add(detailTile("كود العميل", code, "hash", Theme.PRIMARY));
        grid.add(detailTile("الهاتف", phone == null || phone.isBlank() ? "—" : phone, "phone", Theme.SUCCESS));
        grid.add(detailTile("العنوان", address == null || address.isBlank() ? "—" : address, "map-pin", Theme.NAVY_3));
        grid.add(detailTile("الرصيد الافتتاحي", money(account.openingBalance().toPlainString()), "coins",
                Theme.WARNING));
        card.add(grid);
        return card;
    }

    /** Asks for confirmation with the real activity counts, then deletes the customer and all its records. */
    private void deleteCustomer(Customer customer) {
        if (customerService == null || customer == null)
            return;
        new SwingWorker<CustomerActivityCounts, Void>() {
            protected CustomerActivityCounts doInBackground() throws Exception {
                return customerService.findActivityCounts(customer.code());
            }

            protected void done() {
                try {
                    confirmCustomerDelete(customer, get());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل بيانات العميل: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void confirmCustomerDelete(Customer customer, CustomerActivityCounts counts) {
        int receipts = Math.max(0, counts.statementEntries() - counts.invoices());
        String message = "سيتم حذف العميل \"" + customer.name() + "\" نهائيًا مع كل العمليات الخاصة به:\n"
                + "• الفواتير: " + counts.invoices() + "\n"
                + "• التحصيلات: " + receipts + "\n"
                + "• المنتجات: " + counts.products() + "\n\n"
                + "لا يمكن التراجع عن هذه العملية. هل تريد المتابعة؟";
        Object[] options = { "حذف نهائي", "إلغاء" };
        int choice = JOptionPane.showOptionDialog(this, message, "حذف العميل", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0)
            return;
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                customerService.delete(customer.id());
                return null;
            }

            protected void done() {
                try {
                    get();
                    toast.showMessage("تم حذف العميل " + customer.name() + " وكل العمليات الخاصة به");
                    customers();
                } catch (Exception exception) {
                    toast.showMessage("تعذر حذف العميل: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    /**
     * Customer detail tile: rounded tinted background, a fixed 42px gradient circle with the icon
     * exactly centred, then caption and value centred vertically beside it.
     */
    private JPanel detailTile(String title, String value, String icon, Color accent) {
        Color tint = Animator.blend(Theme.CARD, accent, 0.15);
        JPanel tile = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(tint);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.setColor(Animator.blend(Theme.CARD, accent, 0.45));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        Icon glyph = Icons.icon(icon, 20, Color.WHITE);
        JComponent circle = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = 42;
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
                g.fillOval(x, y + 2, d, d);
                g.setPaint(new GradientPaint(x, y, Animator.blend(accent, Color.WHITE, 0.25), x + d, y + d,
                        Animator.blend(accent, Color.BLACK, 0.20)));
                g.fillOval(x, y, d, d);
                glyph.paintIcon(this, g, x + (d - glyph.getIconWidth()) / 2, y + (d - glyph.getIconHeight()) / 2);
                g.dispose();
            }
        };
        Dimension size = new Dimension(44, 46);
        circle.setPreferredSize(size);
        circle.setMinimumSize(size);
        tile.add(circle, BorderLayout.EAST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        JLabel caption = label(title, 11, Font.BOLD, Animator.blend(accent, Theme.NAVY, 0.45));
        JLabel content = label(value, 14, Font.BOLD, Theme.NAVY);
        content.setToolTipText(value);
        text.add(caption);
        text.add(content);
        // the tile takes its share of the row; long values end with "…" (full text in the tooltip)
        text.setPreferredSize(new Dimension(60, text.getPreferredSize().height));
        text.setMinimumSize(new Dimension(20, text.getPreferredSize().height));
        tile.add(text, BorderLayout.CENTER);
        return tile;
    }

    /** Detail item with a tinted round icon, caption and bold value (customer page). */
    private JPanel iconInfo(String title, String value, String icon, boolean highlight) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(highlight);
        item.setBackground(Theme.BABY_LIGHT);
        item.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JLabel badge = new JLabel(Icons.icon(icon, 18, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.NAVY_3));
                g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(38, 38));
        item.add(badge, BorderLayout.EAST);
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(label(title, 11, Font.PLAIN, Theme.MUTED_TEXT), BorderLayout.NORTH);
        text.add(label(value, 13, Font.BOLD, Theme.NAVY));
        item.add(text, BorderLayout.CENTER);
        return item;
    }

    private JPanel info(String title, String value, boolean highlight) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(highlight ? new Color(0xF1, 0xF7, 0xFD) : Theme.CARD);
        if (highlight)
            p.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0x5B, 0x96, 0xCE)));
        JLabel a = label(title, 11, Font.PLAIN, Theme.MUTED_TEXT);
        JLabel b = label(value, 12, Font.BOLD, Theme.NAVY);
        p.add(a, BorderLayout.NORTH);
        p.add(b);
        return p;
    }

    private JPanel customerStatement() {
        AppCard card = new AppCard();
        JPanel title = new JPanel(new BorderLayout());
        title.setBackground(Theme.CARD);
        title.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
        JPanel copy = new JPanel(new GridLayout(2, 1));
        copy.setOpaque(false);
        JLabel statementTitle = label("كشف الحساب", 15, Font.BOLD, Theme.NAVY);
        statementTitle.setIcon(new GradientIconBadge("receipt", 30));
        statementTitle.setIconTextGap(9);
        copy.add(statementTitle);
        copy.add(label("حركة الفواتير والتحصيلات — الصفوف السابقة لا يتم تعديلها", 11, Font.PLAIN,
                Theme.MUTED_TEXT));
        title.add(copy, BorderLayout.EAST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        DatePickerField startDate = new DatePickerField(java.time.LocalDate.now().minusMonths(12));
        DatePickerField endDate = new DatePickerField(java.time.LocalDate.now());
        actions.add(labeledDate("من", startDate));
        actions.add(labeledDate("إلى", endDate));
        JButton applyDates = iconAction("عرض الفترة", AppButton.Kind.SECONDARY, "search", () -> { });
        actions.add(applyDates);
        actions.add(iconAction("إضافة تحصيل", AppButton.Kind.PRIMARY, "wallet", this::receiptDialog));
        title.add(actions, BorderLayout.WEST);
        // quick periods
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presets.setOpaque(false);
        String[][] periods = { { "هذا الشهر", "month" }, { "آخر 3 شهور", "quarter" }, { "هذه السنة", "year" },
                { "كل الفترات", "all" } };
        JPanel presetsRow = new JPanel(new BorderLayout());
        presetsRow.setOpaque(false);
        presetsRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        presetsRow.add(presets, BorderLayout.WEST);
        title.add(presetsRow, BorderLayout.SOUTH);
        card.add(title, BorderLayout.NORTH);
        JTable statement = table(new String[] { "التاريخ", "البيان", "رقم الفاتورة", "مدين", "دائن", "المديونية" },
                new Object[0][6]);
        statement.setRowHeight(48);
        DefaultTableModel statementModel = (DefaultTableModel) statement.getModel();
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.CARD);
        JScrollPane statementScroll = new JScrollPane(statement);
        statementScroll.setPreferredSize(new Dimension(0, statement.getTableHeader().getPreferredSize().height
                + (statement.getRowHeight() * PaginationBar.DEFAULT_PAGE_SIZE) + 2));
        center.add(statementScroll);
        JLabel debitTotal = label(money("0"), 11, Font.BOLD, Theme.TEXT);
        JLabel creditTotal = label(money("0"), 11, Font.BOLD, new Color(0x17, 0x82, 0x49));
        JLabel balanceTotal = label(money("0"), 11, Font.BOLD, Theme.TEXT);
        JPanel totals = new JPanel(new GridLayout(1, 4, 20, 0));
        totals.setBackground(new Color(0xF1, 0xF5, 0xF9));
        totals.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        totals.add(label("إجمالي الحركات", 11, Font.BOLD, Theme.TEXT));
        totals.add(debitTotal);
        totals.add(creditTotal);
        totals.add(balanceTotal);
        center.add(totals, BorderLayout.SOUTH);
        PaginationBar statementPagination = new PaginationBar();
        card.add(center);
        card.add(statementPagination, BorderLayout.SOUTH);
        java.util.List<StatementEntry>[] statementRows = new java.util.List[] { new java.util.ArrayList<>() };
        int[] statementPage = { 0 };
        Runnable[] renderStatement = { null };
        renderStatement[0] = () -> {
            statementModel.setRowCount(0);
            int start = statementPage[0] * PaginationBar.DEFAULT_PAGE_SIZE;
            int end = Math.min(start + PaginationBar.DEFAULT_PAGE_SIZE, statementRows[0].size());
            for (int i = start; i < end; i++) {
                StatementEntry row = statementRows[0].get(i);
                statementModel.addRow(new Object[] { row.date(), row.description(), row.invoiceNumber(),
                        money(row.debit().toPlainString()), money(row.credit().toPlainString()),
                        money(row.balance().toPlainString()) });
            }
            statementPagination.update(statementRows[0].size(), statementPage[0]);
        };
        statementPagination.setPageListener(page -> {
            statementPage[0] = page;
            renderStatement[0].run();
        });
        Runnable refresh = () -> {
            final java.time.LocalDate from;
            final java.time.LocalDate to;
            from = startDate.date();
            to = endDate.date();
            if (from == null || to == null) {
                toast.showMessage("أدخل التاريخ بصيغة yyyy-MM-dd أو اختره من التقويم");
                return;
            }
            if (from.isAfter(to)) {
                toast.showMessage("بداية الفترة يجب أن تسبق نهايتها");
                return;
            }
            statementModel.setRowCount(0);
            debitTotal.setText(money("0"));
            creditTotal.setText(money("0"));
            balanceTotal.setText(money("0"));
            if (invoiceService != null)
                new SwingWorker<java.util.List<StatementEntry>, Void>() {
                    @Override
                    protected java.util.List<StatementEntry> doInBackground() throws Exception {
                        return invoiceService.findCustomerStatement(selectedCustomerCode, from, to);
                    }

                    @Override
                    protected void done() {
                        try {
                            java.util.List<StatementEntry> rows = get();
                            statementRows[0] = rows;
                            statementPage[0] = 0;
                            BigDecimal debit = BigDecimal.ZERO;
                            BigDecimal credit = BigDecimal.ZERO;
                            for (StatementEntry row : rows) {
                                debit = debit.add(row.debit());
                                credit = credit.add(row.credit());
                            }
                            debitTotal.setText(money(debit.toPlainString()));
                            creditTotal.setText(money(credit.toPlainString()));
                            balanceTotal.setText(
                                    money(rows.isEmpty() ? "0" : rows.get(rows.size() - 1).balance().toPlainString()));
                            renderStatement[0].run();
                        } catch (Exception exception) {
                            toast.showMessage("تعذر تحميل كشف حساب العميل: " + exception.getMessage());
                        }
                    }
                }.execute();
        };
        applyDates.addActionListener(event -> refresh.run());
        startDate.setOnChange(refresh);
        endDate.setOnChange(refresh);
        for (String[] period : periods) {
            JButton chip = new JButton(period[0]);
            chip.setFont(Theme.font(Font.BOLD, 11));
            chip.setForeground(Theme.PRIMARY_DARK);
            chip.setBackground(Theme.BABY_LIGHT);
            chip.putClientProperty("JButton.buttonType", "roundRect");
            chip.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(event -> {
                java.time.LocalDate today = java.time.LocalDate.now();
                startDate.setDate(switch (period[1]) {
                    case "month" -> today.withDayOfMonth(1);
                    case "quarter" -> today.minusMonths(3);
                    case "year" -> today.withDayOfYear(1);
                    default -> java.time.LocalDate.of(2000, 1, 1);
                });
                endDate.setDate(today);
                refresh.run();
            });
            presets.add(chip);
        }
        refresh.run();
        return card;
    }

    private void receiptDialog() {
        if (invoiceService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        AppModalDialog dialog = new AppModalDialog(this, "إضافة تحصيل", "file");
        JPanel body = dialog.body();
        body.setLayout(new MigLayout("fillx,insets 0,wrap 2,rtl,gap 13", "[grow,fill][grow,fill]", ""));
        DatePickerField date = new DatePickerField(java.time.LocalDate.now());
        FormField amount = new FormField("المبلغ *", "0.00", null);
        body.add(modalField("التاريخ *", date), "growx,aligny top");
        body.add(amount, "growx");
        dialog.addFooterButton(action("إلغاء", AppButton.Kind.SECONDARY, dialog::dispose));
        dialog.addFooterButton(action("حفظ", AppButton.Kind.PRIMARY, () -> {
            final java.time.LocalDate receiptDate;
            final BigDecimal receiptAmount;
            receiptDate = date.date();
            if (receiptDate == null) {
                toast.showMessage("اختر التاريخ من التقويم أو اكتبه بصيغة yyyy-MM-dd");
                return;
            }
            try {
                receiptAmount = new BigDecimal(amount.text()).setScale(2, java.math.RoundingMode.UNNECESSARY);
            } catch (NumberFormatException | ArithmeticException exception) {
                amount.showValidation("أدخل مبلغاً صحيحاً وبمنزلتين عشريتين");
                return;
            }
            if (receiptAmount.compareTo(BigDecimal.ZERO) <= 0) {
                amount.showValidation("يجب أن يكون المبلغ أكبر من صفر");
                return;
            }
            new SwingWorker<com.cartonapp.model.Receipt, Void>() {
                @Override
                protected com.cartonapp.model.Receipt doInBackground() throws Exception {
                    return invoiceService.createReceipt(selectedCustomerCode, null, receiptDate, receiptAmount,
                            "نقدي", null);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        toast.showMessage("تمت إضافة التحصيل ومزامنة كشف الحساب");
                        customer();
                    } catch (Exception exception) {
                        toast.showMessage("تعذر حفظ التحصيل");
                    }
                }
            }.execute();
        }));
        Rtl.apply(body);
        dialog.setPreferredSize(null);
        dialog.pack();
        dialog.setSize(570, dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void printCustomerStatement() {
        if (invoiceService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        new SwingWorker<CustomerPrintData, Void>() {
            @Override
            protected CustomerPrintData doInBackground() throws Exception {
                java.util.List<StatementEntry> entries = invoiceService.findCustomerStatement(selectedCustomerCode);
                String name = customerService == null ? selectedCustomerCode
                        : customerService.findByCode(selectedCustomerCode).map(Customer::name)
                                .orElse(selectedCustomerCode);
                return new CustomerPrintData(name, entries);
            }

            @Override
            protected void done() {
                try {
                    CustomerPrintData data = get();
                    if (statementPrintService.print(data.name(), data.entries()))
                        toast.showMessage("تم إرسال كشف الحساب إلى الطابعة");
                } catch (Exception exception) {
                    toast.showMessage("تعذر طباعة كشف الحساب: " + exception.getMessage());
                }
            }
        }.execute();
    }

    private JPanel customerProductsTab() {
        AppCard card = new AppCard();
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Theme.CARD);
        head.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        head.add(label("منتجات العميل", 14, Font.BOLD, Theme.TEXT), BorderLayout.EAST);
        JLabel title = label("جارٍ تحميل المنتجات المسجلة…", 11, Font.PLAIN, Theme.MUTED_TEXT);
        head.add(title, BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);
        JTable products = table(new String[] { "المسلسل", "اسم المنتج", "المقاس", "سعر الكرتونة" },
                new Object[0][4]);
        DefaultTableModel model = (DefaultTableModel) products.getModel();
        setColumnWidths(products, 75, 350, 220, 170);
        products.putClientProperty("rowClickable", Boolean.TRUE);
        PaginationBar productsPagination = new PaginationBar();
        card.add(new JScrollPane(products));
        card.add(productsPagination, BorderLayout.SOUTH);
        java.util.List<ProductSummary>[] productRows = new java.util.List[] { new java.util.ArrayList<>() };
        int[] productPage = { 0 };
        Runnable[] renderProducts = { null };
        renderProducts[0] = () -> {
            model.setRowCount(0);
            int start = productPage[0] * PaginationBar.DEFAULT_PAGE_SIZE;
            int end = Math.min(start + PaginationBar.DEFAULT_PAGE_SIZE, productRows[0].size());
            for (int i = start; i < end; i++) {
                ProductSummary row = productRows[0].get(i);
                model.addRow(new Object[] { i + 1, row.name(), row.dimensions(),
                        money(row.currentPrice().toPlainString()) });
            }
            productsPagination.update(productRows[0].size(), productPage[0]);
        };
        productsPagination.setPageListener(page -> {
            productPage[0] = page;
            renderProducts[0].run();
        });
        if (productService != null)
            new SwingWorker<java.util.List<ProductSummary>, Void>() {
                @Override
                protected java.util.List<ProductSummary> doInBackground() throws Exception {
                    return productService.findByCustomerCode(selectedCustomerCode);
                }

                @Override
                protected void done() {
                    try {
                        java.util.List<ProductSummary> rows = get();
                        productRows[0] = rows;
                        productPage[0] = 0;
                        renderProducts[0].run();
                        title.setText("المنتجات المسجلة للعميل: " + rows.size());
                        onCellClick(products, (viewRow, column) -> {
                            int index = productPage[0] * PaginationBar.DEFAULT_PAGE_SIZE
                                    + products.convertRowIndexToModel(viewRow);
                            if (index < rows.size()) {
                                selectedProduct = rows.get(index);
                                product();
                            }
                        });
                    } catch (Exception exception) {
                        title.setText("تعذر تحميل المنتجات المسجلة");
                    }
                };
            }.execute();
        return card;
    }

    private JPanel customerInvoicesTab() {
        AppCard card = new AppCard();
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Theme.CARD);
        head.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        head.add(label("فواتير العميل", 14, Font.BOLD, Theme.TEXT), BorderLayout.EAST);
        head.add(iconAction("فاتورة جديدة", AppButton.Kind.PRIMARY, "plus", this::invoiceForSelectedCustomer),
                BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);
        JTable invoices = table(
                new String[] { "رقم الفاتورة", "التاريخ", "الصافي", "المتبقي" },
                new Object[0][4]);
        setColumnWidths(invoices, 200, 150, 160, 160);
        invoices.putClientProperty("rowClickable", Boolean.TRUE);
        DefaultTableModel model = (DefaultTableModel) invoices.getModel();
        PaginationBar pagination = new PaginationBar();
        java.util.List<InvoiceSummary>[] invoiceRows = new java.util.List[] { new java.util.ArrayList<>() };
        int[] invoicePage = { 0 };
        Runnable[] render = { null };
        render[0] = () -> {
            model.setRowCount(0);
            int start = invoicePage[0] * PaginationBar.DEFAULT_PAGE_SIZE;
            int end = Math.min(start + PaginationBar.DEFAULT_PAGE_SIZE, invoiceRows[0].size());
            for (int i = start; i < end; i++) {
                InvoiceSummary row = invoiceRows[0].get(i);
                model.addRow(new Object[] { row.number(), row.date(), money(row.total().toPlainString()),
                        money(row.remaining().toPlainString()) });
            }
            pagination.update(invoiceRows[0].size(), invoicePage[0]);
        };
        pagination.setPageListener(page -> {
            invoicePage[0] = page;
            render[0].run();
        });
        onCellClick(invoices, (viewRow, column) -> {
            int index = invoicePage[0] * PaginationBar.DEFAULT_PAGE_SIZE + invoices.convertRowIndexToModel(viewRow);
            if (index < invoiceRows[0].size())
                openInvoicePreview(invoiceRows[0].get(index).id());
        });
        JScrollPane invoiceScroll = new JScrollPane(invoices);
        invoiceScroll.setPreferredSize(new Dimension(0, invoices.getTableHeader().getPreferredSize().height
                + (invoices.getRowHeight() * PaginationBar.DEFAULT_PAGE_SIZE) + 2));
        card.add(invoiceScroll);
        card.add(pagination, BorderLayout.SOUTH);
        if (invoiceService != null)
            new SwingWorker<java.util.List<InvoiceSummary>, Void>() {
                @Override
                protected java.util.List<InvoiceSummary> doInBackground() throws Exception {
                    return invoiceService.findAll();
                }

                @Override
                protected void done() {
                    try {
                        java.util.List<InvoiceSummary> all = get();
                        invoiceRows[0] = all.stream().filter(row -> selectedCustomerCode.equals(row.customerCode()))
                                .toList();
                        invoicePage[0] = 0;
                        render[0].run();
                    } catch (Exception exception) {
                        toast.showMessage("تعذر تحميل فواتير العميل");
                    }
                }
            }.execute();
        return card;
    }

    private void products() {
        topNavigation.select("products");
        if (productService == null) {
            showProducts(java.util.List.of());
            return;
        }
        JPanel loading = screen("دليل المنتجات", "المنتجات", "جارٍ تحميل بيانات المنتجات…");
        loading.add(new SkeletonPanel(), "growx");
        setPage(loading);
        new SwingWorker<java.util.List<ProductSummary>, Void>() {
            @Override
            protected java.util.List<ProductSummary> doInBackground() throws Exception {
                return productService.findAll();
            }

            @Override
            protected void done() {
                try {
                    showProducts(get());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل المنتجات من قاعدة البيانات");
                    showProducts(java.util.List.of());
                }
            }
        }.execute();
    }

    private void showProducts(java.util.List<ProductSummary> rows) {
        currentScreen = "products";
        JPanel p = screen("دليل المنتجات", "المنتجات", rows.size() + " منتجات مسجلة",
                action("تصدير Excel", AppButton.Kind.SECONDARY, () -> exportReport(false, ReportService::productList)),
                action("تصدير PDF", AppButton.Kind.SECONDARY, () -> exportReport(true, ReportService::productList)),
                action("+ منتج جديد", AppButton.Kind.PRIMARY, this::productDialog));
        AppCard c = new AppCard();
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 11));
        b.setBackground(Theme.CARD);
        JTextField search = searchField("بحث باسم المنتج أو العميل");
        JLabel shown = label("عرض " + rows.size() + " منتجات", 11, Font.PLAIN, Theme.MUTED_TEXT);
        b.add(search);
        b.add(Box.createHorizontalStrut(180));
        b.add(shown);
        c.add(b, BorderLayout.NORTH);
        JTable t = table(new String[] { "المسلسل", "الاسم", "العميل", "النشاط", "المقاس", "سعر الكرتونة",
                "تعديل", "حذف" }, new Object[0][8]);
        t.putClientProperty("rowClickable", Boolean.TRUE);
        t.setAutoCreateRowSorter(false);
        t.setRowSorter(null);
        setColumnWidths(t, 75, 240, 225, 175, 185, 140, 58, 58);
        t.getColumnModel().getColumn(6)
                .setCellRenderer(new TableActionRenderer("تعديل المنتج", "edit", Theme.PRIMARY));
        t.getColumnModel().getColumn(7)
                .setCellRenderer(new TableActionRenderer("حذف المنتج", "delete", Theme.DANGER));
        java.util.List<ProductSummary> filteredRows = new java.util.ArrayList<>();
        int[] currentPage = { 0 };
        Runnable[] render = { null };
        PaginationBar pagination = new PaginationBar();
        render[0] = () -> {
            filteredRows.clear();
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            for (ProductSummary row : rows) {
                boolean matchesQuery = query.isEmpty() || row.name().toLowerCase(Locale.ROOT).contains(query)
                        || row.customerName().toLowerCase(Locale.ROOT).contains(query);
                if (matchesQuery)
                    filteredRows.add(row);
            }
            int pageCount = Math.max(1,
                    (filteredRows.size() + PaginationBar.DEFAULT_PAGE_SIZE - 1) / PaginationBar.DEFAULT_PAGE_SIZE);
            currentPage[0] = Math.min(currentPage[0], pageCount - 1);
            int start = currentPage[0] * PaginationBar.DEFAULT_PAGE_SIZE;
            int end = Math.min(start + PaginationBar.DEFAULT_PAGE_SIZE, filteredRows.size());
            DefaultTableModel model = (DefaultTableModel) t.getModel();
            model.setRowCount(0);
            for (int i = start; i < end; i++) {
                ProductSummary row = filteredRows.get(i);
                model.addRow(new Object[] { i + 1, row.name(), row.customerName(), row.customerIndustry(),
                        row.dimensions(), money(row.currentPrice().toPlainString()), "تعديل", "حذف" });
            }
            shown.setText(recordRange(filteredRows.size(), currentPage[0]));
            pagination.update(filteredRows.size(), currentPage[0]);
        };
        pagination.setPageListener(page -> {
            currentPage[0] = page;
            render[0].run();
        });
        DocumentListener productFilters = change(() -> {
            currentPage[0] = 0;
            render[0].run();
        });
        search.getDocument().addDocumentListener(productFilters);
        onCellClick(t, (viewRow, modelColumn) -> {
            if (viewRow >= t.getRowCount())
                return;
            int modelRow = t.convertRowIndexToModel(viewRow);
            int index = currentPage[0] * PaginationBar.DEFAULT_PAGE_SIZE + modelRow;
            if (index < 0 || index >= filteredRows.size())
                return;
            selectedProduct = filteredRows.get(index);
            Object clickedValue = t.getModel().getValueAt(modelRow, modelColumn);
            if ("تعديل".equals(clickedValue))
                editProductDialog();
            else if ("حذف".equals(clickedValue))
                deleteSelectedProduct();
            else
                product();
            t.clearSelection();
        });
        JScrollPane tableScroll = new JScrollPane(t);
        tableScroll.setPreferredSize(new Dimension(0,
                t.getTableHeader().getPreferredSize().height
                        + (t.getRowHeight() * PaginationBar.DEFAULT_PAGE_SIZE) + 2));
        c.add(tableScroll);
        c.add(pagination, BorderLayout.SOUTH);
        p.add(c, "grow,push");
        setPage(p);
        render[0].run();
    }

    private record ProductView(ProductSummary product, java.util.List<com.cartonapp.model.PricePoint> history,
            java.util.List<com.cartonapp.model.ProductSale> sales) {
    }

    /** Loads the selected product with its price history and sales, then shows the product page. */
    private void product() {
        topNavigation.select("products");
        if (selectedProduct == null || productService == null) {
            products();
            return;
        }
        String code = selectedProduct.code();
        JPanel productLoading = screen("المنتجات / " + code, selectedProduct.name(), "جارٍ تحميل بيانات المنتج…");
        productLoading.add(new SkeletonPanel(), "growx");
        setPage(productLoading);
        new SwingWorker<ProductView, Void>() {
            protected ProductView doInBackground() throws Exception {
                ProductSummary fresh = productService.findByCode(code).orElse(selectedProduct);
                return new ProductView(fresh, productService.findPriceHistory(fresh.id()),
                        productService.findSales(fresh.id()));
            }

            protected void done() {
                try {
                    ProductView view = get();
                    selectedProduct = view.product();
                    showProduct(view);
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل بيانات المنتج: " + rootMessage(exception));
                    products();
                }
            }
        }.execute();
    }

    private void showProduct(ProductView view) {
        currentScreen = "product";
        ProductSummary product = view.product();
        String code = product.code();
        com.cartonapp.service.ProductPricingService.Breakdown pricing = com.cartonapp.service.ProductPricingService
                .breakdown(product.lengthCm(), product.widthCm(), product.heightCm());
        JPanel subtitle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        subtitle.setOpaque(false);
        subtitle.add(new StatusBadge(code, StatusBadge.Tone.BLUE));
        subtitle.add(label("العميل: " + product.customerName(), 12, Font.PLAIN, Theme.MUTED_TEXT));
        JPanel p = screen("المنتجات / " + code, product.name(), subtitle,
                squareIconAction("back", this::products),
                iconAction("تعديل", AppButton.Kind.SECONDARY, "edit", this::editProductDialog),
                iconAction("حذف", AppButton.Kind.DANGER, "delete", this::deleteSelectedProduct),
                iconAction("تصدير PDF", AppButton.Kind.SECONDARY, "download",
                        () -> exportReport(true, reports -> reports.product(code))),
                iconAction("تصدير Excel", AppButton.Kind.SECONDARY, "download",
                        () -> exportReport(false, reports -> reports.product(code))),
                action("إعادة التسعير", AppButton.Kind.PRIMARY, () -> repriceProduct(product)));

        // Key figures
        BigDecimal netSales = BigDecimal.ZERO;
        BigDecimal soldQuantity = BigDecimal.ZERO;
        for (com.cartonapp.model.ProductSale sale : view.sales()) {
            InvoiceLine line = new InvoiceLine(0, 0, product.id(), sale.quantity(), "", sale.unitPrice(), sale.discount());
            netSales = netSales.add(com.cartonapp.service.InvoiceCalculator.lineNet(line));
            soldQuantity = soldQuantity.add(sale.quantity());
        }
        JPanel metrics = new JPanel(new GridLayout(1, 4, 11, 0));
        metrics.setOpaque(false);
        metrics.add(new MetricCard("سعر الكرتونة الحالي", money(product.currentPrice().toPlainString()),
                MetricCard.Tone.BLUE));
        metrics.add(new MetricCard("السعر حسب المعادلة اليوم", money(pricing.price().toPlainString()),
                MetricCard.Tone.NEUTRAL));
        metrics.add(new MetricCard("الكمية المباعة", quantityText(soldQuantity) + " (" + view.sales().size() + " فاتورة)",
                MetricCard.Tone.ORANGE));
        metrics.add(new MetricCard("صافي المبيعات", money(netSales.toPlainString()), MetricCard.Tone.GREEN));
        p.add(metrics, "growx,wrap");
        if (product.currentPrice().compareTo(pricing.price()) != 0) {
            JLabel notice = label("للعلم: السعر الحالي مُدخل يدويًا، والسعر حسب معادلة التسعير اليوم "
                    + money(pricing.price().toPlainString()) + ". يمكنك اعتماده بزر \"إعادة التسعير\".",
                    11, Font.PLAIN, new Color(0x8A, 0x55, 0x00));
            notice.setOpaque(true);
            notice.setBackground(new Color(0xFF, 0xF6, 0xE5));
            notice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(0xE5, 0x9C, 0x36)),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            p.add(notice, "growx,wrap");
        }

        JPanel layout = new JPanel(new MigLayout("fillx,insets 0,gap 13", "[grow,fill][320!,fill]", "[top]"));
        layout.setOpaque(false);
        JPanel main = new JPanel(new MigLayout("fillx,insets 0,wrap,gap 13", "[grow,fill]", ""));
        main.setOpaque(false);
        main.add(productSpecificationsCard(product, pricing), "growx");
        main.add(productSalesCard(view.sales(), product), "growx");
        JPanel side = new JPanel(new MigLayout("fillx,insets 0,wrap,gap 13", "[grow,fill]", ""));
        side.setOpaque(false);
        side.add(productFormulaCard(pricing), "growx");
        side.add(priceHistoryCard(view.history()), "growx");
        layout.add(main, "cell 0 0,growx,top");
        layout.add(side, "cell 1 0,growx,top");
        p.add(layout, "growx,wrap");
        setPage(p);
    }

    /** Saves today's formula price as the product's current price (adds a price-history entry). */
    private void repriceProduct(ProductSummary product) {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                BigDecimal formula = com.cartonapp.service.ProductPricingService.calculate(product.lengthCm(),
                        product.widthCm(), product.heightCm());
                productService.update(new ProductSummary(product.id(), product.code(), product.name(),
                        product.customerCode(), product.customerName(), product.customerIndustry(), product.lengthCm(),
                        product.widthCm(), product.heightCm(), formula, product.boxType(), product.layerCount(),
                        product.printColors(), product.specifications()));
                return null;
            }

            protected void done() {
                try {
                    get();
                    toast.showMessage("تم اعتماد السعر المحسوب للمنتج " + product.code());
                    product();
                } catch (Exception exception) {
                    toast.showMessage("تعذرت إعادة التسعير: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private AppCard productSpecificationsCard(ProductSummary product,
            com.cartonapp.service.ProductPricingService.Breakdown pricing) {
        Color purple = new Color(0x7B, 0x5C, 0xD6);
        Color teal = new Color(0x0E, 0x93, 0xA6);
        JPanel content = new JPanel(new MigLayout("fillx,insets 14,wrap 1,gap 0 10", "[grow,fill]"));
        content.setBackground(Theme.CARD);
        JPanel product1 = new JPanel(new GridLayout(2, 3, 12, 12));
        product1.setOpaque(false);
        product1.add(detailTile("كود المنتج", product.code(), "hash", Theme.PRIMARY));
        product1.add(detailTile("العميل", product.customerName(), "user", Theme.NAVY_3));
        product1.add(detailTile("نوع العلبة", product.boxType(), "box", Theme.SUCCESS));
        product1.add(detailTile("المقاس (طول × عرض × ارتفاع)", product.dimensions(), "ruler", Theme.PRIMARY_DARK));
        product1.add(detailTile("عدد الطبقات", product.layerCount() + " طبقات", "layers", Theme.WARNING));
        product1.add(detailTile("ألوان الطباعة",
                product.printColors() == 0 ? "بدون طباعة" : product.printColors() + " لون", "palette", purple));
        content.add(product1);
        JPanel specs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        specs.setOpaque(false);
        JLabel specsTitle = label("المواصفات:", 12, Font.BOLD, Theme.NAVY);
        specsTitle.setIcon(Icons.icon("layers", 15, Theme.PRIMARY));
        specsTitle.setIconTextGap(6);
        specs.add(specsTitle);
        java.util.Map<String, Integer> productSpecs = product.specificationCounts();
        if (productSpecs.isEmpty())
            specs.add(label("لم تُحدَّد مواصفات", 12, Font.PLAIN, Theme.MUTED_TEXT));
        boolean firstSpec = true;
        for (java.util.Map.Entry<String, Integer> spec : productSpecs.entrySet()) {
            if (!firstSpec)
                specs.add(label("في", 13, Font.BOLD, Theme.MUTED_TEXT));
            specs.add(specChip(spec.getKey(), spec.getValue() + " " + spec.getKey()));
            firstSpec = false;
        }
        specs.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        content.add(specs);
        JLabel calculated = label("محسوب تلقائيًا من المقاس", 12, Font.BOLD, teal);
        calculated.setIcon(Icons.icon("chart", 15, teal));
        calculated.setIconTextGap(6);
        calculated.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        content.add(calculated);
        JPanel sheet = new JPanel(new GridLayout(1, 3, 12, 0));
        sheet.setOpaque(false);
        sheet.add(detailTile("طول الشيت", plainNumber(pricing.sheetLength()) + " سم", "ruler", teal));
        sheet.add(detailTile("عرض الشيت", plainNumber(pricing.sheetWidth()) + " سم", "ruler", teal));
        sheet.add(detailTile("مساحة الشيت", plainNumber(pricing.sheetArea()) + " م²", "layers", teal));
        content.add(sheet);
        return productCard("مواصفات الكرتونة", content,
                iconAction("تعديل", AppButton.Kind.GHOST, "edit", this::editProductDialog));
    }

    /** Coloured pill with a check mark for one paper specification. */
    private static JComponent specChip(String spec, String text) {
        Color color = switch (spec) {
            case "كرافت" -> new Color(0xA0, 0x6A, 0x2C);
            case "فلوت" -> Theme.PRIMARY;
            default -> Theme.SUCCESS;
        };
        JLabel chip = new JLabel(text, Icons.icon("shield", 14, Color.WHITE), SwingConstants.RIGHT) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Animator.blend(color, Color.WHITE, 0.15), 0, getHeight(),
                        Animator.blend(color, Color.BLACK, 0.12)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        chip.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        chip.setIconTextGap(6);
        chip.setFont(Theme.font(Font.BOLD, 12));
        chip.setForeground(Color.WHITE);
        chip.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        return chip;
    }

    /** The pricing formula applied to this carton, step by step. */
    private AppCard productFormulaCard(com.cartonapp.service.ProductPricingService.Breakdown pricing) {
        JPanel rows = new JPanel(new GridLayout(0, 1));
        rows.setBackground(Theme.CARD);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 17, 12, 17));
        rows.add(priceRow("مساحة الشيت", plainNumber(pricing.sheetArea()) + " م²", false, false));
        rows.add(priceRow("سعر الطن (طبقات + مصنعية)", money(pricing.pricePerTon().toPlainString()), false, false));
        rows.add(priceRow("الجرام (متوسط + 7)", plainNumber(pricing.grammage()), false, false));
        rows.add(priceRow("معامل التعريج", plainNumber(pricing.corrugationFactor()), false, false));
        rows.add(priceRow("سعر الكرتونة المحسوب", money(pricing.price().toPlainString()), false, true));
        JLabel formula = label("المساحة × سعر الطن × المعامل × الجرام ÷ 1,000,000", 11, Font.BOLD, Theme.NOTE_TEXT);
        formula.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        rows.add(formula);
        return productCard("معادلة التسعير لهذا المنتج", rows, null);
    }

    private AppCard productSalesCard(java.util.List<com.cartonapp.model.ProductSale> sales, ProductSummary product) {
        if (sales.isEmpty()) {
            JLabel empty = label("لم يُبَع هذا المنتج في أي فاتورة بعد.", 12, Font.PLAIN, Theme.MUTED_TEXT);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(28, 15, 28, 15));
            return productCard("مبيعات المنتج", empty, null);
        }
        JTable table = table(new String[] { "رقم الفاتورة", "التاريخ", "العميل", "الكمية", "الوحدة", "الصافي" },
                new Object[0][6]);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int shown = Math.min(sales.size(), 8);
        for (int i = 0; i < shown; i++) {
            com.cartonapp.model.ProductSale sale = sales.get(i);
            InvoiceLine line = new InvoiceLine(0, 0, product.id(), sale.quantity(), "", sale.unitPrice(), sale.discount());
            model.addRow(new Object[] { sale.invoiceNumber(), sale.date().toString(), sale.customerName(),
                    quantityText(sale.quantity()), money(sale.unitPrice().toPlainString()),
                    money(com.cartonapp.service.InvoiceCalculator.lineNet(line).toPlainString()) });
        }
        table.setRowHeight(40);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(1, table.getTableHeader().getPreferredSize().height + shown * 40 + 2));
        JLabel note = label(sales.size() > shown ? "آخر " + shown + " من " + sales.size() + " عملية بيع — التفاصيل كاملة في تصدير المنتج"
                : sales.size() + " عملية بيع", 11, Font.PLAIN, Theme.MUTED_TEXT);
        return productCard("مبيعات المنتج", scroll, null, note);
    }

    private AppCard priceHistoryCard(java.util.List<com.cartonapp.model.PricePoint> history) {
        JPanel rows = new JPanel(new GridLayout(0, 1));
        rows.setBackground(Theme.CARD);
        rows.setBorder(BorderFactory.createEmptyBorder(2, 15, 10, 15));
        if (history.isEmpty())
            rows.add(label("لا يوجد سجل أسعار.", 11, Font.PLAIN, Theme.MUTED_TEXT));
        int shown = Math.min(history.size(), 8);
        for (int i = 0; i < shown; i++) {
            com.cartonapp.model.PricePoint point = history.get(i);
            BigDecimal previous = i + 1 < history.size() ? history.get(i + 1).price() : null;
            rows.add(historyRow(point.date().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    previous == null ? null : money(previous.toPlainString()), money(point.price().toPlainString())));
        }
        return productCard("تاريخ الأسعار", rows, null);
    }

    private JPanel historyRow(String date, String oldPrice, String newPrice) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.CARD);
        row.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.PRICE_DIVIDER),
                        BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        row.add(label(date, 12, Font.BOLD, Theme.MUTED_TEXT), BorderLayout.EAST);
        JPanel prices = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        prices.setOpaque(false);
        if (oldPrice == null) {
            prices.add(label("السعر الأول", 11, Font.BOLD, Theme.NOTE_TEXT));
        } else {
            JLabel old = label(oldPrice, 11, Font.PLAIN, Theme.NOTE_TEXT);
            old.setFont(old.getFont().deriveFont(java.util.Map.of(java.awt.font.TextAttribute.STRIKETHROUGH,
                    java.awt.font.TextAttribute.STRIKETHROUGH_ON)));
            prices.add(old);
            prices.add(new JLabel(Icons.icon("back", 14, Theme.MUTED_TEXT)));
        }
        prices.add(label(newPrice, 11, Font.BOLD, Theme.PRICE_VALUE));
        row.add(prices, BorderLayout.WEST);
        return row;
    }

    private JPanel productInfo(String caption, String value, boolean calculated) {
        JPanel info = new JPanel(new BorderLayout(0, 4));
        info.setBackground(calculated ? Theme.CALCULATED_FIELD : Theme.CARD);
        info.setBorder(calculated ? BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, Theme.CALCULATED_ACCENT),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)) : BorderFactory.createEmptyBorder(7, 0, 7, 0));
        info.add(label(caption, 11, Font.BOLD, Theme.MUTED_TEXT), BorderLayout.NORTH);
        info.add(label(value == null || value.isBlank() ? "—" : value, 12, Font.BOLD, Theme.TEXT));
        return info;
    }

    private AppCard productCard(String title, Component content, JButton titleAction) {
        return productCard(title, content, titleAction, null);
    }

    private AppCard productCard(String title, Component content, JButton titleAction, JComponent footer) {
        AppCard card = new AppCard();
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(Theme.SECTION);
        head.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 4, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(11, 15, 11, 15)));
        head.add(label(title, 14, Font.BOLD, Theme.NAVY), BorderLayout.EAST);
        if (titleAction != null)
            head.add(titleAction, BorderLayout.WEST);
        else if (footer != null)
            head.add(footer, BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);
        card.add(content);
        return card;
    }

    private JPanel priceRow(String name, String amount, boolean total, boolean grand) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
                grand ? BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.FIELD_BORDER)
                        : BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.PRICE_DIVIDER),
                BorderFactory.createEmptyBorder(grand ? 10 : 8, 0, 8, 0)));
        Color color = grand ? Theme.PRICE_VALUE : (total ? Theme.TEXT : Theme.PRICE_ROW_TEXT);
        row.add(label(name, grand ? 13 : 11, Font.BOLD, color), BorderLayout.EAST);
        row.add(label(amount, grand ? 14 : 11, Font.BOLD, grand ? Theme.PRICE_VALUE : Theme.TEXT), BorderLayout.WEST);
        return row;
    }

    private static String plainNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String quantityText(BigDecimal value) {
        return new java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    private JButton squareIconAction(String icon, Runnable action) {
        JButton button = new AppButton("", AppButton.Kind.SECONDARY, icon);
        button.setPreferredSize(new Dimension(36, 34));
        button.setMinimumSize(new Dimension(36, 34));
        button.setMaximumSize(new Dimension(36, 34));
        button.addActionListener(event -> action.run());
        return button;
    }

    private static final class GradientPanel extends JPanel {
        GradientPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new GradientPaint(0, 0, Theme.PRICE_HEAD_START, getWidth(), getHeight(), Theme.PRICE_HEAD_END));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private void invoices() {
        topNavigation.select("invoices");
        if (invoiceService == null) {
            toast.showMessage("إعدادات قاعدة البيانات غير متاحة");
            showInvoices(java.util.List.of());
            return;
        }
        JPanel loading = screen("المبيعات والتحصيل", "الفواتير", "جارٍ تحميل الفواتير…");
        loading.add(new SkeletonPanel(), "growx");
        setPage(loading);
        new SwingWorker<java.util.List<InvoiceSummary>, Void>() {
            protected java.util.List<InvoiceSummary> doInBackground() throws Exception {
                return invoiceService.findAll();
            }

            protected void done() {
                try {
                    showInvoices(get());
                } catch (Exception e) {
                    toast.showMessage("تعذر تحميل الفواتير: " + e.getMessage());
                    showInvoices(java.util.List.of());
                }
            }
        }.execute();
    }

    private void showInvoices(java.util.List<InvoiceSummary> rows) {
        JPanel p = screen("المبيعات والتحصيل", "الفواتير", rows.size() + " فواتير مسجلة",
                iconAction("فاتورة جديدة", AppButton.Kind.PRIMARY, "plus", this::invoiceDialog));
        BigDecimal total = rows.stream().filter(i -> "POSTED".equals(i.status())).map(InvoiceSummary::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = rows.stream().map(InvoiceSummary::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 11, 0));
        metrics.setOpaque(false);
        metrics.add(metric("إجمالي مرحّل", money(total.toPlainString()), Theme.PRIMARY));
        metrics.add(metric("المحصّل", money(paid.toPlainString()), new Color(0x3C, 0xA4, 0x69)));
        BigDecimal remaining = rows.stream().filter(i -> "POSTED".equals(i.status())).map(InvoiceSummary::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.add(metric("المتبقي", money(remaining.toPlainString()), new Color(0xE5, 0x9C, 0x36)));
        p.add(metrics, "growx,wrap");
        AppCard card = new AppCard();
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 11));
        bar.setBackground(Theme.CARD);
        JTextField search = searchField("بحث برقم الفاتورة أو العميل");
        JLabel count = label(rows.size() + " فواتير", 11, Font.PLAIN, Theme.MUTED_TEXT);
        bar.add(search);
        bar.add(Box.createHorizontalStrut(180));
        bar.add(count);
        card.add(bar, BorderLayout.NORTH);
        JTable table = table(new String[] { "رقم الفاتورة", "العميل", "المدفوع", "الباقي", "التاريخ",
                "تعديل", "حذف" }, new Object[0][7]);
        setColumnWidths(table, 180, 245, 140, 140, 130, 58, 58);
        table.putClientProperty("rowClickable", Boolean.TRUE);
        table.setRowHeight(48);
        table.getTableHeader().setPreferredSize(new Dimension(1, 40));
        table.getColumnModel().getColumn(5)
                .setCellRenderer(new TableActionRenderer("تعديل الفاتورة", "edit", Theme.PRIMARY));
        table.getColumnModel().getColumn(6)
                .setCellRenderer(new TableActionRenderer("حذف الفاتورة", "delete", Theme.DANGER));
        java.util.List<InvoiceSummary> filteredRows = new java.util.ArrayList<>();
        int[] currentPage = { 0 };
        int pageSize = PaginationBar.DEFAULT_PAGE_SIZE;
        Runnable[] render = { null };
        PaginationBar pagination = new PaginationBar();
        render[0] = () -> {
            filteredRows.clear();
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            for (InvoiceSummary row : rows) {
                boolean matchesQuery = query.isEmpty() || row.number().toLowerCase(Locale.ROOT).contains(query)
                        || row.customerName().toLowerCase(Locale.ROOT).contains(query);
                if (matchesQuery)
                    filteredRows.add(row);
            }
            int pageCount = Math.max(1, (filteredRows.size() + pageSize - 1) / pageSize);
            currentPage[0] = Math.min(currentPage[0], pageCount - 1);
            int start = currentPage[0] * pageSize;
            int end = Math.min(start + pageSize, filteredRows.size());
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            for (int i = start; i < end; i++) {
                InvoiceSummary row = filteredRows.get(i);
                model.addRow(new Object[] { row.number(), row.customerName(), money(row.paid().toPlainString()),
                        money(row.remaining().toPlainString()), row.date().toString(), "تعديل", "حذف" });
            }
            count.setText(recordRange(filteredRows.size(), currentPage[0]));
            pagination.update(filteredRows.size(), currentPage[0]);
        };
        pagination.setPageListener(page -> {
            currentPage[0] = page;
            render[0].run();
        });
        onCellClick(table, (viewRow, column) -> {
            if (viewRow >= table.getRowCount())
                return;
            int row = table.convertRowIndexToModel(viewRow);
            int index = currentPage[0] * pageSize + row;
            if (index >= filteredRows.size())
                return;
            InvoiceSummary selected = filteredRows.get(index);
            if (column <= 4)
                invoiceDetails(selected);
            else if (column == 5)
                invoiceEditDialog(selected);
            else if (column == 6)
                deleteInvoice(selected.id(), selected.number());
        });
        DocumentListener invoiceFilters = change(() -> {
            currentPage[0] = 0;
            render[0].run();
        });
        search.getDocument().addDocumentListener(invoiceFilters);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, table.getTableHeader().getPreferredSize().height
                + (table.getRowHeight() * PaginationBar.DEFAULT_PAGE_SIZE) + 2));
        card.add(tableScroll);
        card.add(pagination, BorderLayout.SOUTH);
        p.add(card, "grow,push");
        setPage(p);
        render[0].run();
    }

    private void deleteInvoice(long invoiceId, String number) {
        int choice = JOptionPane.showConfirmDialog(this,
                "سيتم حذف الفاتورة " + number + " وإلغاء المدفوع نقداً المسجل عليها، مع الاحتفاظ بسجلها المحاسبي. هل تريد المتابعة؟",
                "حذف الفاتورة", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                invoiceService.cancel(invoiceId);
                return null;
            }

            protected void done() {
                try {
                    get();
                    toast.showMessage("تم حذف الفاتورة " + number + " وتحديث رصيد العميل");
                    invoices();
                } catch (Exception exception) {
                    toast.showMessage("تعذر حذف الفاتورة: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void invoiceDialog() {
        openInvoiceEditor(null);
    }

    /** From the customer page: open the invoice form directly with this customer chosen. */
    private void invoiceForSelectedCustomer() {
        openInvoiceEditor(null, selectedCustomerCode);
    }

    private void invoiceEditDialog(InvoiceSummary value) {
        openInvoiceEditor(value.id());
    }

    private void invoiceDetails(InvoiceSummary value) {
        openInvoicePreview(value.id());
    }

    private record InvoiceEditorData(List<Customer> customers, List<InvoiceEditorDialog.ProductChoice> products,
            InvoiceDocument existing) {
    }

    /** Loads customers, products and (when editing) the saved invoice, then opens the template form. */
    private void openInvoiceEditor(Long invoiceId) {
        openInvoiceEditor(invoiceId, null);
    }

    private void openInvoiceEditor(Long invoiceId, String presetCustomerCode) {
        if (invoiceService == null || customerService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        toast.showMessage(invoiceId == null ? "جارٍ تجهيز فاتورة جديدة…" : "جارٍ تحميل الفاتورة…");
        new SwingWorker<InvoiceEditorData, Void>() {
            @Override
            protected InvoiceEditorData doInBackground() throws Exception {
                List<Customer> customers = customerService.findAll();
                List<InvoiceEditorDialog.ProductChoice> products = new ArrayList<>();
                if (productService != null)
                    for (ProductSummary product : productService.findAll())
                        products.add(new InvoiceEditorDialog.ProductChoice(product.id(),
                                product.name() + " — " + product.code(), product.currentPrice(),
                                product.customerCode()));
                InvoiceDocument existing = invoiceId == null ? null : invoiceService.findDocument(invoiceId);
                return new InvoiceEditorData(customers, products, existing);
            }

            @Override
            protected void done() {
                try {
                    InvoiceEditorData data = get();
                    new InvoiceEditorDialog(MainFrame.this, invoiceService, data.customers(), data.products(),
                            data.existing(), presetCustomerCode, (savedId, preview) -> {
                                toast.showMessage(invoiceId == null ? "تم حفظ الفاتورة الجديدة"
                                        : "تم حفظ تعديلات الفاتورة");
                                if (presetCustomerCode != null)
                                    customer();
                                else
                                    invoices();
                                if (preview)
                                    openInvoicePreview(savedId);
                            }).show();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    toast.showMessage("تعذر فتح نموذج الفاتورة: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    /** Shows the saved invoice exactly as it prints, with print, PDF, Excel, edit and delete actions. */
    private void openInvoicePreview(long invoiceId) {
        if (invoiceService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        new SwingWorker<InvoiceDocument, Void>() {
            @Override
            protected InvoiceDocument doInBackground() throws Exception {
                return invoiceService.findDocument(invoiceId);
            }

            @Override
            protected void done() {
                try {
                    InvoiceDocument document = get();
                    new InvoicePreviewDialog(MainFrame.this, document, companyProfile, invoicePrintService,
                            invoicePdfService, invoiceExportService,
                            edited -> openInvoiceEditor(edited.id()),
                            deleted -> deleteInvoice(deleted.id(), deleted.number())).setVisible(true);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    toast.showMessage("تعذر تحميل الفاتورة: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null)
            cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private void settings() {
        topNavigation.select("settings");
        JPanel p = screen("تخصيص البرنامج", "الإعدادات", "بيانات الشركة وكلمة المرور ومعادلة التسعير والنظام");
        if (settingsService == null) {
            p.add(label("تعذر الوصول إلى قاعدة البيانات. تأكد من ملف db.properties.", 12, Font.PLAIN, Theme.ERROR),
                    "growx,wrap");
        } else {
            p.add(new SettingsPanel(settingsService, toast, this::useCompany), "growx,wrap");
        }
        setPage(p);
    }

    private void customerDialog() {
        customerDialog(null);
    }

    private void editCustomerDialog() {
        if (customerService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        new SwingWorker<Customer, Void>() {
            protected Customer doInBackground() throws Exception {
                return customerService.findByCode(selectedCustomerCode).orElseThrow();
            }

            protected void done() {
                try {
                    customerDialog(get());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل بيانات العميل");
                }
            }
        }.execute();
    }

    private void customerDialog(Customer current) {
        boolean editing = current != null;
        AppModalDialog dialog = new AppModalDialog(this, editing ? "تعديل العميل" : "عميل جديد", "file");
        dialog.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel body = dialog.body();
        body.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        body.setLayout(new MigLayout("fillx,insets 0,wrap 2,rtl,gap 13", "[grow,fill][grow,fill]", ""));
        FormField name = new FormField("الاسم *", editing ? current.name() : "اسم العميل أو الشركة", null, false,
                !editing);
        FormField industry = new FormField("النشاط", editing ? current.industry() : "مثال: أجهزة منزلية", null, false,
                !editing);
        FormField phone = new FormField("الهاتف", editing ? current.phone() : "01xx xxx xxxx", null, false, !editing);
        FormField address = new FormField("العنوان", editing ? current.address() : "عنوان العميل", null, false,
                !editing);
        FormField opening = new FormField("المديونية السابقة (رصيد افتتاحي)",
                editing ? current.openingBalance().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00",
                "ج.م", false, !editing);
        body.add(name, "growx");
        body.add(industry, "growx");
        body.add(phone, "growx");
        body.add(opening, "growx");
        body.add(address, "growx,span 2");
        JPanel validation = new JPanel(new BorderLayout());
        validation.setBackground(new Color(0xF7, 0xF9, 0xFB));
        validation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.MUTED_TEXT),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        validation.add(label("الحقول المعلّمة بـ * مطلوبة للحفظ — المديونية السابقة تُضاف إلى رصيد العميل وتظهر في الرصيد السابق لكل فاتورة",
                10, Font.PLAIN, Theme.MUTED_TEXT));
        body.add(validation, "growx,span 2,gaptop 0");
        dialog.addFooterButton(action("إلغاء", AppButton.Kind.SECONDARY, dialog::dispose));
        dialog.addFooterButton(action("حفظ", AppButton.Kind.PRIMARY,
                () -> saveCustomer(dialog, current, name, industry, phone, address, opening)));
        Rtl.apply(body);
        dialog.remove(body);
        JScrollPane formScroll = new JScrollPane(body);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.getViewport().setBackground(Theme.CARD);
        dialog.add(formScroll, BorderLayout.CENTER);
        // Height follows the form content instead of a fixed 500px, so no empty band under the note.
        dialog.setPreferredSize(null);
        dialog.pack();
        dialog.setSize(570, dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveCustomer(AppModalDialog dialog, Customer current, FormField name, FormField industry,
            FormField phone, FormField address, FormField opening) {
        if (name.isBlank()) {
            name.showValidation("اسم العميل مطلوب");
            return;
        }
        if (industry.isBlank()) {
            industry.showValidation("النشاط مطلوب");
            return;
        }
        if (address.isBlank()) {
            address.showValidation("العنوان مطلوب");
            return;
        }
        final BigDecimal openingBalance;
        try {
            openingBalance = parseOpeningBalance(opening.text());
        } catch (NumberFormatException exception) {
            opening.showValidation("أدخل مبلغًا صحيحًا بحد أقصى منزلتين عشريتين");
            return;
        }
        if (customerService == null) {
            toast.showMessage("تعذر الوصول إلى قاعدة البيانات");
            return;
        }
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                if (current == null)
                    customerService.create(name.text(), industry.text(), phone.text(), address.text(), openingBalance);
                else
                    customerService.update(new Customer(current.id(), current.code(), name.text(), industry.text(),
                            phone.text(), address.text(), openingBalance));
                return null;
            }

            protected void done() {
                try {
                    get();
                    dialog.dispose();
                    toast.showMessage(current == null ? "تمت إضافة العميل بنجاح" : "تم تحديث بيانات العميل بنجاح");
                    // An edit made on the customer's page returns to that page, refreshed.
                    if (current != null && "customer".equals(currentScreen)) {
                        selectedCustomerCode = current.code();
                        customer();
                    } else {
                        customers();
                    }
                } catch (Exception exception) {
                    toast.showMessage("تعذر حفظ بيانات العميل");
                }
            }
        }.execute();
    }

    /** Blank means no opening debt; Arabic-Indic digits and thousands separators are accepted. */
    private static BigDecimal parseOpeningBalance(String text) {
        StringBuilder value = new StringBuilder();
        for (char c : text.trim().toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') value.append((char) ('0' + c - '\u0660'));
            else if (c == '\u066B') value.append('.');
            else if (c != ',' && c != '\u066C' && c != ' ') value.append(c);
        }
        if (value.isEmpty())
            return BigDecimal.ZERO.setScale(2);
        BigDecimal amount = new BigDecimal(value.toString());
        if (amount.stripTrailingZeros().scale() > 2)
            throw new NumberFormatException("too many decimals");
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private void productDialog() {
        productDialog(null);
    }

    private void editProductDialog() {
        if (selectedProduct == null) {
            toast.showMessage("اختر المنتج أولاً");
            return;
        }
        productDialog(selectedProduct);
    }

    private void productDialog(ProductSummary current) {
        if (customerService == null) {
            toast.showMessage("تعذر تحميل العملاء من قاعدة البيانات");
            return;
        }
        new SwingWorker<java.util.List<Customer>, Void>() {
            @Override
            protected java.util.List<Customer> doInBackground() throws Exception {
                return customerService.findAll();
            }

            @Override
            protected void done() {
                try {
                    productDialog(current, get());
                } catch (Exception exception) {
                    toast.showMessage("تعذر تحميل العملاء: " + exception.getMessage());
                }
            }
        }.execute();
    }

    private void productDialog(ProductSummary current, java.util.List<Customer> customers) {
        boolean editing = current != null;
        AppModalDialog dialog = new AppModalDialog(this, editing ? "تعديل المنتج" : "منتج جديد", "file");
        dialog.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel body = dialog.body();
        body.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        body.setLayout(new MigLayout("fillx,insets 0,wrap,gap 12", "[grow]", ""));
        JPanel basic = dialog.section("البيانات الأساسية");
        JPanel basicGrid = new JPanel(new MigLayout("fillx,insets 13,wrap 2,rtl,gap 13", "[grow,fill][grow,fill]", ""));
        basicGrid.setBackground(Theme.CARD);
        String[] customerOptions = new String[customers.size() + 1];
        customerOptions[0] = "اختر العميل";
        for (int i = 0; i < customers.size(); i++) {
            Customer customerValue = customers.get(i);
            customerOptions[i + 1] = customerValue.code() + " — " + customerValue.name();
        }
        JComboBox<String> customer = FormField.combo(customerOptions);
        customer.setPreferredSize(new Dimension(1, 34));
        if (editing)
            for (int i = 0; i < customers.size(); i++)
                if (customers.get(i).code().equals(current.customerCode()))
                    customer.setSelectedIndex(i + 1);
        JPanel customerField = modalField("العميل *", customer);
        JLabel customerValidation = label("", 10, Font.PLAIN, Theme.ERROR);
        customerValidation.setVisible(false);
        customerField.add(customerValidation, BorderLayout.SOUTH);
        basicGrid.add(customerField, "growx");
        FormField productName = new FormField("الاسم *", editing ? current.name() : "اسم المنتج", null, false,
                !editing);
        basicGrid.add(productName, "growx");
        FormField boxType = new FormField("نوع العلبة", editing ? current.boxType() : "كرتونة شق عادي RSC", null, false,
                !editing);
        basicGrid.add(boxType, "growx");
        basic.add(basicGrid);
        body.add(basic, "growx");
        JPanel dimensions = dialog.section("الأبعاد والمواصفات");
        JPanel dimensionsGrid = new JPanel(
                new MigLayout("fillx,insets 13,wrap 4,rtl,gap 13", "[grow,fill][grow,fill][grow,fill][grow,fill]", ""));
        dimensionsGrid.setBackground(Theme.CARD);
        FormField length = new FormField("الطول", editing ? current.lengthCm().toPlainString() : "42", "سم");
        FormField width = new FormField("العرض", editing ? current.widthCm().toPlainString() : "31", "سم");
        FormField height = new FormField("الارتفاع", editing ? current.heightCm().toPlainString() : "36", "سم");
        FormField layersCount = new FormField("عدد الطبقات", editing ? String.valueOf(current.layerCount()) : "5",
                null);
        FormField colors = new FormField("ألوان الطباعة", editing ? String.valueOf(current.printColors()) : "2", null);
        FormField manualPrice = new FormField("سعر الكرتونة *",
                editing ? current.currentPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00",
                "ج.م", false, !editing);
        JLabel formulaHint = label("", 11, Font.BOLD, Theme.MUTED_TEXT);
        JButton useFormula = iconAction("استخدام السعر حسب المعادلة", AppButton.Kind.GHOST, "edit",
                () -> manualPrice.setText(calculatedProductPrice(length, width, height)));
        dimensionsGrid.add(length, "growx");
        dimensionsGrid.add(width, "growx");
        dimensionsGrid.add(height, "growx");
        dimensionsGrid.add(layersCount, "growx");
        dimensionsGrid.add(colors, "growx");
        Runnable refreshCalculatedPrice = () -> formulaHint
                .setText("للاسترشاد فقط — السعر حسب معادلة التسعير: " + calculatedProductPrice(length, width, height) + " ج.م");
        refreshCalculatedPrice.run();
        length.getInput().getDocument().addDocumentListener(change(refreshCalculatedPrice));
        width.getInput().getDocument().addDocumentListener(change(refreshCalculatedPrice));
        height.getInput().getDocument().addDocumentListener(change(refreshCalculatedPrice));
        dimensions.add(dimensionsGrid);
        body.add(dimensions, "growx");
        JPanel calculatedSection = dialog.section("سعر الكرتونة");
        JPanel priceGrid = new JPanel(new MigLayout("fillx,insets 13,wrap 2,rtl,gap 13", "[grow,fill][grow,fill]", ""));
        priceGrid.setBackground(Theme.CARD);
        priceGrid.add(manualPrice, "growx");
        JPanel hintBox = new JPanel(new BorderLayout(0, 4));
        hintBox.setOpaque(false);
        hintBox.add(formulaHint, BorderLayout.NORTH);
        JPanel hintAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        hintAction.setOpaque(false);
        hintAction.add(useFormula);
        hintBox.add(hintAction, BorderLayout.CENTER);
        priceGrid.add(hintBox, "growx,aligny top");
        calculatedSection.add(priceGrid, BorderLayout.CENTER);
        body.add(calculatedSection, "growx");
        JPanel specsSection = dialog.section("المواصفات");
        JPanel specsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 22, 12));
        specsRow.setBackground(Theme.CARD);
        java.util.List<JCheckBox> specBoxes = new java.util.ArrayList<>();
        java.util.Map<String, Integer> ticked = editing ? current.specificationCounts() : java.util.Map.of();
        for (String spec : ProductSummary.SPECIFICATIONS) {
            JCheckBox box = new JCheckBox(spec, ticked.containsKey(spec));
            box.setFont(Theme.font(Font.BOLD, 13));
            box.setForeground(Theme.NAVY);
            box.setOpaque(false);
            box.setIconTextGap(8);
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // number box beside each specification, e.g. 2 كرافت
            JTextField count = new JTextField(String.valueOf(ticked.getOrDefault(spec, 1)));
            count.setHorizontalAlignment(SwingConstants.CENTER);
            count.setFont(Theme.font(Font.BOLD, 13));
            count.setForeground(Theme.PRIMARY_DARK);
            count.setPreferredSize(new Dimension(52, Theme.FIELD_HEIGHT));
            count.setToolTipText("عدد " + spec);
            count.setEnabled(box.isSelected());
            box.addActionListener(e -> {
                count.setEnabled(box.isSelected());
                if (box.isSelected()) {
                    if (count.getText().isBlank()) count.setText("1");
                    count.requestFocusInWindow();
                    count.selectAll();
                }
            });
            box.putClientProperty("specCount", count);
            JPanel pair = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            pair.setOpaque(true);
            pair.setBackground(Theme.SECTION);
            pair.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            pair.add(box);
            pair.add(count);
            specBoxes.add(box);
            specsRow.add(pair);
        }
        JLabel specsHint = label("اختر المواصفة واكتب العدد بجانبها، مثل: 2 كرافت في 2 فلوت", 11,
                Font.PLAIN, Theme.MUTED_TEXT);
        specsHint.setIcon(Icons.icon("layers", 14, Theme.PRIMARY));
        specsHint.setIconTextGap(6);
        specsHint.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        specsSection.add(specsRow, BorderLayout.CENTER);
        specsSection.add(specsHint, BorderLayout.SOUTH);
        // right under the basic data, so it is visible without scrolling
        body.add(specsSection, "growx", 1);
        dialog.addFooterButton(action("إلغاء", AppButton.Kind.SECONDARY, dialog::dispose));
        dialog.addFooterButton(action("حفظ", AppButton.Kind.PRIMARY, () -> saveProduct(dialog, current, customers,
                customer, customerValidation, productName, boxType, length, width, height, layersCount, colors,
                manualPrice, specBoxes)));
        Rtl.apply(body);
        dialog.remove(body);
        JScrollPane formScroll = new JScrollPane(body);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.getViewport().setBackground(Theme.CARD);
        dialog.add(formScroll, BorderLayout.CENTER);
        dialog.setSize(860, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String calculatedProductPrice(FormField length, FormField width, FormField height) {
        try {
            return com.cartonapp.service.ProductPricingService.calculate(new BigDecimal(length.text()),
                    new BigDecimal(width.text()), new BigDecimal(height.text())).toPlainString();
        } catch (RuntimeException exception) {
            return "0.00";
        }
    }

    private void saveProduct(AppModalDialog dialog, ProductSummary current, java.util.List<Customer> customers,
            JComboBox<String> customer, JLabel validation, FormField name, FormField boxType, FormField length,
            FormField width, FormField height, FormField layers, FormField colors, FormField price,
            java.util.List<JCheckBox> specBoxes) {
        boolean missingCustomer = customer.getSelectedIndex() == 0 && current == null;
        validation.setText("العميل مطلوب");
        validation.setVisible(missingCustomer);
        if (name.isBlank()) {
            name.showValidation("اسم المنتج مطلوب");
            return;
        }
        if (missingCustomer || productService == null) {
            return;
        }
        if (boxType.isBlank()) {
            boxType.showValidation("نوع العلبة مطلوب");
            return;
        }
        BigDecimal lengthValue = parseDecimal(length, "أدخل الطول أكبر من صفر", BigDecimal.ONE, 2);
        if (lengthValue == null)
            return;
        BigDecimal widthValue = parseDecimal(width, "أدخل العرض أكبر من صفر", BigDecimal.ONE, 2);
        if (widthValue == null)
            return;
        BigDecimal heightValue = parseDecimal(height, "أدخل الارتفاع أكبر من صفر", BigDecimal.ONE, 2);
        if (heightValue == null)
            return;
        Integer layersValue = parseInteger(layers, "أدخل عدد الطبقات من 1 إلى 20", 1, 20);
        if (layersValue == null)
            return;
        Integer colorsValue = parseInteger(colors, "أدخل عدد ألوان من 1 إلى 6", 1, 6);
        if (colorsValue == null)
            return;
        BigDecimal enteredPrice = parseDecimal(price, "أدخل سعرًا أكبر من صفر وبحد أقصى منزلتين عشريتين",
                new BigDecimal("0.01"), 2);
        if (enteredPrice == null)
            return;
        java.util.List<String> specParts = new java.util.ArrayList<>();
        for (JCheckBox box : specBoxes) {
            if (!box.isSelected())
                continue;
            JTextField countField = (JTextField) box.getClientProperty("specCount");
            int specCount;
            try {
                specCount = Integer.parseInt(countField.getText().trim());
            } catch (NumberFormatException exception) {
                specCount = 0;
            }
            if (specCount < 1 || specCount > 99) {
                countField.putClientProperty("JComponent.outline", "error");
                toast.showMessage("اكتب عدد " + box.getText() + " رقمًا صحيحًا من 1 إلى 99");
                return;
            }
            countField.putClientProperty("JComponent.outline", null);
            specParts.add(box.getText() + ":" + specCount);
        }
        try {
            Customer selectedCustomer = customers.get(customer.getSelectedIndex() - 1);
            String customerName = selectedCustomer.name();
            String customerCode = selectedCustomer.code();
            BigDecimal priceValue = enteredPrice.setScale(2, java.math.RoundingMode.UNNECESSARY);
            ProductSummary value = new ProductSummary(current == null ? 0 : current.id(),
                    current == null ? "" : current.code(), name.text(), customerCode, customerName,
                    selectedCustomer.industry(), lengthValue, widthValue, heightValue, priceValue, boxType.text(),
                    layersValue, colorsValue, String.join(",", specParts));
            new SwingWorker<ProductSummary, Void>() {
                protected ProductSummary doInBackground() throws Exception {
                    if (current == null)
                        return productService.create(value.customerCode(), value.name(), value.boxType(),
                                value.lengthCm(), value.widthCm(), value.heightCm(), value.layerCount(),
                                value.printColors(), value.currentPrice(), value.specifications());
                    productService.update(value);
                    return value;
                }

                protected void done() {
                    try {
                        selectedProduct = get();
                        dialog.dispose();
                        toast.showMessage(current == null ? "تمت إضافة المنتج ومزامنته" : "تم تحديث المنتج ومزامنته");
                        // An edit made on the product's page returns to that page, refreshed.
                        if (current != null && "product".equals(currentScreen))
                            product();
                        else
                            products();
                    } catch (Exception exception) {
                        toast.showMessage("تعذر حفظ المنتج: " + exception.getMessage());
                    }
                }
            }.execute();
        } catch (RuntimeException exception) {
            toast.showMessage("تعذر تجهيز بيانات المنتج للحفظ");
        }
    }

    private String customerCodeForName(String name) {
        return switch (name) {
            case "الصفوة للأجهزة المنزلية" -> "C-001";
            case "النيل للصناعات الغذائية" -> "C-002";
            case "أولاد منصور للتجارة" -> "C-003";
            case "الدلتا للمنظفات" -> "C-004";
            case "رويال للصناعات الدوائية" -> "C-005";
            case "المتحدة للزيوت" -> "C-006";
            default -> selectedProduct == null ? "C-001" : selectedProduct.customerCode();
        };
    }

    private String customerIndustryForName(String name) {
        return switch (name) {
            case "الصفوة للأجهزة المنزلية" -> "أجهزة منزلية";
            case "النيل للصناعات الغذائية" -> "صناعات غذائية";
            case "أولاد منصور للتجارة" -> "تجارة وتوزيع";
            case "الدلتا للمنظفات" -> "منظفات";
            case "رويال للصناعات الدوائية" -> "أدوية";
            case "المتحدة للزيوت" -> "زيوت";
            default -> selectedProduct == null ? "" : selectedProduct.customerIndustry();
        };
    }

    private void deleteSelectedProduct() {
        if (selectedProduct == null || productService == null)
            return;
        int choice = JOptionPane.showConfirmDialog(this,
                "سيتم إخفاء المنتج من القوائم مع بقاء مراجع الفواتير محفوظة. هل تريد المتابعة؟", "حذف المنتج",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION)
            return;
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                productService.deactivate(selectedProduct.id());
                return null;
            }

            protected void done() {
                try {
                    get();
                    toast.showMessage("تم حذف المنتج من القوائم ومزامنة البيانات");
                    products();
                } catch (Exception e) {
                    toast.showMessage("تعذر حذف المنتج");
                }
            }
        }.execute();
    }

    private JPanel modalField(String title, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel caption = label(title, 11, Font.BOLD, Theme.LABEL_TEXT);
        caption.setIcon(FieldIcons.iconFor(title, 14));
        caption.setIconTextGap(6);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(field);
        return panel;
    }

    /** "من" / "إلى" written beside the field, so dates and buttons share one row and one height. */
    private JPanel labeledDate(String title, DatePickerField picker) {
        JPanel panel = new JPanel(new BorderLayout(7, 0));
        panel.setOpaque(false);
        JLabel caption = label(title, 12, Font.BOLD, Theme.NAVY);
        caption.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(caption, BorderLayout.EAST);
        panel.add(picker, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, Theme.FIELD_HEIGHT));
        return panel;
    }

    private void dialog(String title, String[][] fields) {
        JDialog d = new JDialog(this, title, true);
        d.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.add(form(fields));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(action("حفظ", AppButton.Kind.PRIMARY, () -> {
            d.dispose();
            toast.showMessage("تم الحفظ بنجاح");
        }));
        buttons.add(action("إلغاء", AppButton.Kind.SECONDARY, d::dispose));
        p.add(buttons, BorderLayout.SOUTH);
        Rtl.apply(p);
        d.setContentPane(p);
        d.setSize(title.equals("منتج جديد") ? 860 : 570, 520);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private JPanel screen(String eyebrow, String title, String subtitle, JButton... actions) {
        return screen(eyebrow, title, label(subtitle, 12, Font.PLAIN, Theme.MUTED_TEXT), actions);
    }

    private JPanel screen(String eyebrow, String title, Component subtitle, JButton... actions) {
        JPanel p = new JPanel(new MigLayout("fillx,insets 16 28 20 28,wrap", "[grow]", "[]12[]"));
        p.setOpaque(false);
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JPanel t = new JPanel(new MigLayout("insets 0,wrap 2", "[grow]", "[][]"));
        t.setOpaque(false);
        t.add(label(eyebrow, 11, Font.BOLD, Theme.PRIMARY));
        JLabel heading = label(title, 24, Font.BOLD, Theme.NAVY);
        heading.setIcon(new GradientIconBadge(pageIcon(eyebrow + " " + title), 40));
        heading.setIconTextGap(12);
        t.add(heading, "wrap");
        t.add(subtitle);
        h.add(t, BorderLayout.EAST);
        JPanel a = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        a.setOpaque(false);
        for (JButton b : actions)
            a.add(b);
        h.add(a, BorderLayout.WEST);
        p.add(h, "growx");
        return p;
    }

    /** Icon for a page header, chosen from its eyebrow and title. */
    private static String pageIcon(String text) {
        if (text.contains("الإعدادات") || text.contains("تخصيص")) return "gear";
        if (text.contains("الفواتير") || text.contains("المبيعات")) return "receipt";
        if (text.contains("المنتجات")) return "box";
        if (text.contains("العملاء")) return "users";
        return "chart";
    }

    /** Rounded blue-to-navy tile with a white icon (page headers). */
    static final class GradientIconBadge implements Icon {
        private final Icon icon;
        private final int size;

        GradientIconBadge(String name, int size) {
            this.icon = Icons.icon(name, size / 2, Color.WHITE);
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x1E, 0x6F, 0xD9, 50));
            g.fillRoundRect(x + 1, y + 4, size - 2, size - 2, 14, 14);
            g.setPaint(new GradientPaint(x, y, Theme.PRIMARY, x + size, y + size, Theme.NAVY_2));
            g.fillRoundRect(x, y, size, size - 2, 14, 14);
            icon.paintIcon(c, g, x + size / 4, y + size / 4 - 1);
            g.dispose();
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size; }
    }

    private JPanel toolbar(String s) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 11));
        p.setBackground(Theme.CARD);
        JTextField f = new JTextField(s, 28);
        f.setPreferredSize(new Dimension(340, 34));
        p.add(f);
        return p;
    }

    private JPanel section(String title, Component c) {
        AppCard card = new AppCard();
        JLabel h = label(title, 14, Font.BOLD, Theme.NAVY);
        h.setOpaque(true);
        h.setBackground(Theme.SECTION);
        h.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 4, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        card.add(h, BorderLayout.NORTH);
        card.add(c);
        return card;
    }

    private JPanel paginatedTable(Component table, int total) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(table, BorderLayout.CENTER);
        PaginationBar bar = new PaginationBar();
        bar.update(total, 0);
        panel.add(bar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel form(String[][] values) {
        JPanel p = new JPanel(new GridLayout(0, 2, 13, 13));
        p.setBackground(Theme.CARD);
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        for (String[] v : values) {
            JPanel f = new JPanel(new BorderLayout(0, 5));
            f.setBackground(Theme.CARD);
            f.add(label(v[0], 11, Font.BOLD, Theme.LABEL_TEXT), BorderLayout.NORTH);
            f.add(new JTextField(v[1]));
            p.add(f);
        }
        return p;
    }

    private JPanel metric(String s, String v, Color c) {
        MetricCard.Tone tone = c.equals(Theme.PRIMARY) ? MetricCard.Tone.BLUE
                : c.getGreen() > c.getRed() ? MetricCard.Tone.GREEN : MetricCard.Tone.ORANGE;
        return new MetricCard(s, v, tone);
    }

    private JTable table(String[] cols, Object[][] rows) {
        JTable t = new JTable(new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        }) {
            private int hoverRow = -1;
            {
                addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(java.awt.event.MouseEvent event) {
                        trackHover(event.getPoint());
                    }

                    @Override
                    public void mouseDragged(java.awt.event.MouseEvent event) {
                        trackHover(event.getPoint());
                    }
                });
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent event) {
                        hoverRow = -1;
                        putClientProperty(HOVER_CELL, null);
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }

                    @Override
                    public void mousePressed(java.awt.event.MouseEvent event) {
                        if (!SwingUtilities.isLeftMouseButton(event))
                            return;
                        int row = rowAtPoint(event.getPoint());
                        int column = columnAtPoint(event.getPoint());
                        putClientProperty(PRESSED_CELL, row < 0 || column < 0 ? null : new Point(column, row));
                        repaint();
                    }

                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent event) {
                        putClientProperty(PRESSED_CELL, null);
                        repaint();
                    }
                });
            }

            private void trackHover(Point point) {
                int row = rowAtPoint(point);
                int column = columnAtPoint(point);
                Point cell = row < 0 || column < 0 ? null : new Point(column, row);
                boolean action = cell != null && (getCellRenderer(row, column) instanceof TableActionRenderer
                        || Boolean.TRUE.equals(getClientProperty("rowClickable")));
                setCursor(action ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                if (row != hoverRow || !java.util.Objects.equals(cell, getClientProperty(HOVER_CELL))) {
                    hoverRow = row;
                    putClientProperty(HOVER_CELL, cell);
                    repaint();
                }
            }

            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (!(component instanceof TableActionRenderer) && !isRowSelected(row))
                    component.setBackground(row == hoverRow ? Theme.ROW_HOVER : rowBackground(row));
                return component;
            }
        };
        t.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        TableStyle.apply(t);
        t.setAutoCreateRowSorter(true);
        for (int i = 0; i < cols.length; i++) {
            String column = cols[i];
            DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
            centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            centeredRenderer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            t.getColumnModel().getColumn(i).setCellRenderer(centeredRenderer);
            if (column.equals("تعديل") || column.equals("حذف")) {
                DefaultTableCellRenderer actionHeader = new DefaultTableCellRenderer();
                actionHeader.setHorizontalAlignment(SwingConstants.CENTER);
                actionHeader.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                actionHeader.setOpaque(true);
                actionHeader.setBackground(Theme.TABLE_HEADER);
                actionHeader.setForeground(Theme.TABLE_HEADER_TEXT);
                actionHeader.setFont(Theme.font(Font.BOLD, 12));
                t.getColumnModel().getColumn(i).setHeaderRenderer(actionHeader);
            }
            if (column.contains("المديونية") || column.contains("الصافي") || column.contains("المتبقي")
                    || column.contains("سعر") || column.contains("المدفوع") || column.contains("الباقي")
                    || column.equals("مدين") || column.equals("دائن") || column.contains("القيمة")) {
                // amounts: bold blue so figures stand out
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(SwingConstants.CENTER);
                renderer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                renderer.setFont(Theme.font(Font.BOLD, 13));
                renderer.setForeground(Theme.PRIMARY_DARK);
                t.getColumnModel().getColumn(i).setCellRenderer(renderer);
            } else if (column.equals("الاسم") || column.equals("اسم المنتج") || column.equals("العميل")) {
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(SwingConstants.CENTER);
                renderer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                renderer.setFont(Theme.font(Font.BOLD, 13));
                renderer.setForeground(Theme.NAVY);
                t.getColumnModel().getColumn(i).setCellRenderer(renderer);
            } else if (column.contains("الكود") || column.contains("رقم الفاتورة")) {
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(SwingConstants.CENTER);
                renderer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                renderer.setForeground(new Color(0x17, 0x5E, 0xA8));
                renderer.setFont(Theme.font(Font.BOLD, 12));
                t.getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
            if (!column.isBlank() && !column.equals("تعديل") && !column.equals("حذف"))
                t.getColumnModel().getColumn(i).setHeaderRenderer(new SortHeaderRenderer(column));
        }
        return t;
    }

    private JButton pageButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.PLAIN, 11));
        button.setPreferredSize(new Dimension(30, 30));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setBackground(Theme.CARD);
        button.setForeground(Theme.NAVY);
        return button;
    }

    private JButton pageButton(Icon icon, String tooltip) {
        JButton button = pageButton("");
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        return button;
    }

    private static final class SortHeaderRenderer extends DefaultTableCellRenderer {
        SortHeaderRenderer(String text) {
            setText(text);
            setIcon(Icons.icon("sort", 13, Theme.BABY));
            setIconTextGap(5);
            setHorizontalAlignment(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.TRAILING);
            setVerticalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBackground(Theme.TABLE_HEADER);
            setForeground(Theme.TABLE_HEADER_TEXT);
            setFont(Theme.font(Font.BOLD, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                int row, int column) {
            setText(String.valueOf(value));
            return this;
        }
    }

    private static final String HOVER_CELL = "cartonapp.table.hoverCell";
    private static final String PRESSED_CELL = "cartonapp.table.pressedCell";

    /**
     * Runs {@code action} with (view row, model column) when the left button is pressed and released
     * on the same cell. Unlike mouseClicked, this still fires when the mouse moves slightly during
     * the click. The action runs after the release repaint, so the button visibly returns to normal
     * before any modal dialog opens.
     */
    private static void onCellClick(JTable table, java.util.function.BiConsumer<Integer, Integer> action) {
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            private Point pressed;

            @Override
            public void mousePressed(java.awt.event.MouseEvent event) {
                pressed = cellAt(event);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent event) {
                Point released = cellAt(event);
                boolean same = pressed != null && pressed.equals(released);
                pressed = null;
                if (same) {
                    int viewRow = released.y;
                    int modelColumn = table.convertColumnIndexToModel(released.x);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            action.accept(viewRow, modelColumn);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                            java.awt.Window w = SwingUtilities.getWindowAncestor(table);
                            JOptionPane.showMessageDialog(w, "حدث خطأ: " + ex, "خطأ", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }

            private Point cellAt(java.awt.event.MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event))
                    return null;
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                return row < 0 || column < 0 ? null : new Point(column, row);
            }
        });
    }

    /** Zebra striping shared by all list tables. */
    static Color rowBackground(int row) {
        return row % 2 == 1 ? Theme.ROW_ALT : Theme.CARD;
    }

    /**
     * Action cell drawn as a soft button: light tint of the action colour with a coloured outline
     * icon, a stronger tint on hover, and solid colour with a white icon while pressed.
     */
    private static final class TableActionRenderer extends JLabel implements TableCellRenderer {
        private static final int BUTTON_WIDTH = 34;
        private static final int BUTTON_HEIGHT = 30;
        private final Color actionColor;
        private final Icon normalIcon;
        private final Icon pressedIcon;
        private Color buttonColor;
        private Color outlineColor;
        private boolean hovered;

        TableActionRenderer(String tooltip, String icon, Color actionColor) {
            this.actionColor = actionColor;
            this.buttonColor = actionColor;
            String name = switch (icon) {
                case "chevron" -> "eye";
                case "edit" -> "pencil";
                case "delete" -> "trash";
                default -> icon;
            };
            normalIcon = Icons.icon(name, 16, Color.WHITE);
            pressedIcon = Icons.icon(name, 16, Color.WHITE);
            setText("");
            setIcon(normalIcon);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setOpaque(false);
            setToolTipText(tooltip);
            setForeground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                int row, int column) {
            Point cell = new Point(column, row);
            Point hover = (Point) table.getClientProperty(HOVER_CELL);
            boolean hovered = cell.equals(hover);
            boolean pressed = hovered && cell.equals(table.getClientProperty(PRESSED_CELL));
            // top / bottom colours of the gradient: brighter on hover, deeper while pressed
            buttonColor = pressed ? blend(actionColor, Color.BLACK, 0.80f)
                    : hovered ? blend(actionColor, Color.WHITE, 0.80f) : blend(actionColor, Color.WHITE, 0.90f);
            outlineColor = pressed ? blend(actionColor, Color.BLACK, 0.65f)
                    : blend(actionColor, Color.BLACK, hovered ? 0.92f : 0.80f);
            this.hovered = hovered;
            setIcon(pressed ? pressedIcon : normalIcon);
            boolean rowHovered = hover != null && hover.y == row;
            setBackground(selected || rowHovered ? Theme.ROW_HOVER : rowBackground(row));
            // Shift the icon 1px down while pressed so the click reads as a real button press.
            setBorder(BorderFactory.createEmptyBorder(pressed ? 1 : 0, 0, pressed ? 0 : 1, 0));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            int w = Math.min(BUTTON_WIDTH, getWidth() - 6);
            int h = Math.min(BUTTON_HEIGHT, getHeight() - 6);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            // soft coloured shadow under the button
            g2.setColor(new Color(actionColor.getRed(), actionColor.getGreen(), actionColor.getBlue(), hovered ? 70 : 45));
            g2.fillRoundRect(x + 1, y + 3, w - 2, h - 1, 12, 12);
            g2.setPaint(new GradientPaint(0, y, buttonColor, 0, y + h, outlineColor));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawLine(x + 6, y + 1, x + w - 7, y + 1);
            g2.dispose();
            super.paintComponent(g);
        }

        /** {@code amount} of {@code color} mixed over {@code base}. */
        private static Color blend(Color color, Color base, float amount) {
            return new Color(Math.round(color.getRed() * amount + base.getRed() * (1 - amount)),
                    Math.round(color.getGreen() * amount + base.getGreen() * (1 - amount)),
                    Math.round(color.getBlue() * amount + base.getBlue() * (1 - amount)));
        }
    }

    private JTextField searchField(String placeholder) {
        JTextField field = new JTextField();
        field.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.putClientProperty("JTextField.leadingIcon", Icons.icon("search", 16, Theme.FIELD_ICON));
        field.setPreferredSize(new Dimension(340, Theme.FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(1, Theme.FIELD_HEIGHT));
        field.setFont(Theme.font(Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        return field;
    }

    private void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(widths[i]);
        }
    }

    private void installCustomerFilter(JTable table, JTextField search, JCheckBox debtors, JLabel count, int total) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        Runnable update = () -> {
            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
            String query = search.getText().trim();
            if (!query.isEmpty())
                filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            if (debtors.isSelected())
                filters.add(new RowFilter<Object, Object>() {
                    @Override
                    public boolean include(Entry<? extends Object, ? extends Object> entry) {
                        String amount = String.valueOf(entry.getValue(4)).replace(",", "").trim();
                        try {
                            return new BigDecimal(amount).compareTo(BigDecimal.ZERO) > 0;
                        } catch (NumberFormatException exception) {
                            return false;
                        }
                    }
                });
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
            count.setText("عرض " + sorter.getViewRowCount() + " من " + total);
        };
        search.getDocument().addDocumentListener(change(update));
        debtors.addActionListener(e -> update.run());
    }

    private void installProductFilter(JTable table, JTextField search, JComboBox<String> customers) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        Runnable update = () -> {
            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
            String query = search.getText().trim();
            if (!query.isEmpty())
                filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            String customer = (String) customers.getSelectedItem();
            if (customer != null && !customer.equals("كل العملاء"))
                filters.add(RowFilter.regexFilter(java.util.regex.Pattern.quote(customer), 2));
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        };
        search.getDocument().addDocumentListener(change(update));
        customers.addActionListener(e -> update.run());
    }

    private DocumentListener change(Runnable runnable) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                runnable.run();
            }

            public void removeUpdate(DocumentEvent event) {
                runnable.run();
            }

            public void changedUpdate(DocumentEvent event) {
                runnable.run();
            }
        };
    }

    private JPanel pagination(String text) {
        return pagination(text, false);
    }

    private JPanel pagination(String text, boolean ignored) {
        PaginationBar bar = new PaginationBar();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        int total = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        bar.update(total, 0);
        return bar;
    }

    private JLabel label(String s, int size, int style, Color c) {
        JLabel l = new JLabel(s, SwingConstants.RIGHT);
        l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        l.setFont(Theme.font(style, size));
        l.setForeground(c);
        return l;
    }

    private JPanel customerCountSubtitle(int count) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        panel.add(new StatusBadge(count + " عملاء مسجلين", StatusBadge.Tone.BLUE));
        panel.add(label("قاعدة العملاء", 12, Font.PLAIN, Theme.MUTED_TEXT));
        return panel;
    }

    private JButton action(String s, AppButton.Kind k, Runnable r) {
        JButton b = new AppButton(s, k);
        b.addActionListener(e -> r.run());
        return InvoiceEditorDialog.fitText(b);
    }

    private JButton iconAction(String s, AppButton.Kind k, String icon, Runnable r) {
        JButton b = new AppButton(s, k, icon);
        b.addActionListener(e -> r.run());
        return InvoiceEditorDialog.fitText(b);
    }

    private BigDecimal parseDecimal(FormField field, String message, BigDecimal minimum, int scale) {
        try {
            if (field.isBlank()) {
                field.showValidation("هذا الحقل مطلوب");
                return null;
            }
            BigDecimal value = new BigDecimal(field.text());
            if (value.scale() > scale || value.compareTo(minimum) < 0) {
                field.showValidation(message);
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            field.showValidation(message);
            return null;
        }
    }

    private Integer parseInteger(FormField field, String message, int minimum, int maximum) {
        try {
            if (field.isBlank()) {
                field.showValidation("هذا الحقل مطلوب");
                return null;
            }
            int value = Integer.parseInt(field.text());
            if (value < minimum || value > maximum) {
                field.showValidation(message);
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            field.showValidation(message);
            return null;
        }
    }

    private String recordRange(int total, int zeroBasedPage) {
        if (total == 0)
            return "عرض 0–0 من 0";
        int start = zeroBasedPage * PaginationBar.DEFAULT_PAGE_SIZE + 1;
        int end = Math.min((zeroBasedPage + 1) * PaginationBar.DEFAULT_PAGE_SIZE, total);
        return "عرض " + start + "–" + end + " من " + total;
    }

    /** Money always shows exactly two decimals with thousands separators (project rule). */
    private String money(String s) {
        return new java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols.getInstance(Locale.US))
                .format(new BigDecimal(s).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void setPage(JPanel p) {
        Rtl.apply(p);
        page.removeAll();
        ViewportFillingPanel workspace = new ViewportFillingPanel();
        workspace.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        workspace.add(p, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(workspace);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKGROUND);
        page.setBackground(BACKGROUND);
        Rtl.apply(scroll);
        FadeInPanel fade = new FadeInPanel();
        fade.add(scroll);
        page.add(fade);
        fade.start();
        page.revalidate();
        page.repaint();
    }

    /**
     * Makes every workspace use the full viewport width while keeping vertical
     * scrolling available.
     */
    /** Fades a new page in from the canvas colour (page transition). */
    private static final class FadeInPanel extends JPanel {
        private float cover = 1f;

        FadeInPanel() {
            super(new BorderLayout());
            setBackground(BACKGROUND);
        }

        void start() {
            Animator.animate(240, t -> {
                cover = (float) (1 - t);
                repaint();
            }, null);
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            if (cover <= 0.01f) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cover * 0.85f));
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static final class ViewportFillingPanel extends JPanel implements Scrollable {
        ViewportFillingPanel() {
            super(new BorderLayout());
            setBackground(BACKGROUND);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            int height = Math.max(getHeight(), 1);
            g.setPaint(new GradientPaint(0, 0, Theme.CANVAS, 0, Math.min(height, 900), Theme.CANVAS_2));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
