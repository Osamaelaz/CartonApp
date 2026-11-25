package com.cartonapp.ui;

import com.cartonapp.model.Customer;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceDocumentLine;
import com.cartonapp.model.InvoiceInput;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceSummary;
import com.cartonapp.model.InvoiceTotals;
import com.cartonapp.service.InvoiceCalculator;
import com.cartonapp.service.InvoiceService;
import com.cartonapp.service.InvoiceValidationException;
import com.cartonapp.service.InvoiceValidationService;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 * Create / edit form for a sales invoice. Its sections follow the official template: invoice
 * data, customer, the seven-column item table and the totals block. All amounts are calculated
 * live with {@link InvoiceCalculator}; saving validates with {@link InvoiceValidationService}.
 */
public final class InvoiceEditorDialog {
    /** Product offered in the الصنف list, with its current price. */
    public record ProductChoice(Long id, String label, BigDecimal price, String customerCode) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Called on the Swing thread after a successful save. */
    public interface SaveListener {
        void saved(long invoiceId, boolean openPreview);
    }

    /** Customer entry in the list; toString is what the shared RTL combo renderer displays. */
    private record CustomerChoice(Customer customer) {
        @Override
        public String toString() {
            return customer.code().isEmpty() ? customer.name() : customer.name() + "  (" + customer.code() + ")";
        }
    }

    private static final CustomerChoice NO_CUSTOMER =
            new CustomerChoice(new Customer(0, "", "اختر العميل", "", "", ""));
    private static final String NEW_NUMBER = "يُنشأ تلقائيًا عند الحفظ";

    private final InvoiceService invoices;
    private final List<ProductChoice> products;
    private final InvoiceDocument existing;
    private final SaveListener listener;
    private final InvoiceValidationService validation = new InvoiceValidationService();

    private final AppModalDialog dialog;
    private final JComboBox<CustomerChoice> customer;
    private final DatePickerField date = new DatePickerField(LocalDate.now());
    private final JLabel address = valueLabel("");
    private final JLabel company = valueLabel("");
    private final JPanel linesPanel = new JPanel(new MigLayout("rtl,fillx,insets 0,gap 0 4,wrap 1", "[grow,fill]"));
    private final List<LineEditor> lineEditors = new ArrayList<>();
    private final JTextField additionalDiscount = numberField("0.00");
    private final JTextField cashPaid = numberField("0.00");
    private final JLabel totalQuantity = amountLabel();
    private final JLabel invoiceValue = amountLabel();
    private final JLabel previousBalance = amountLabel();
    private final JLabel totalWithDebt = amountLabel();
    private final JLabel finalBalance = amountLabel();
    private final JPanel messages = new JPanel(new MigLayout("rtl,insets 0,gap 0 2,wrap 1", "[grow,fill]"));
    private final List<JButton> saveButtons = new ArrayList<>();
    private final List<JLabel> tableHeadings = new ArrayList<>();
    private BigDecimal loadedPreviousBalance;
    private int balanceRequest;

    public InvoiceEditorDialog(JFrame owner, InvoiceService invoices, List<Customer> customers,
            List<ProductChoice> products, InvoiceDocument existing, SaveListener listener) {
        this(owner, invoices, customers, products, existing, null, listener);
    }

    /** {@code presetCustomerCode} chooses the customer of a new invoice (opened from a customer page). */
    public InvoiceEditorDialog(JFrame owner, InvoiceService invoices, List<Customer> customers,
            List<ProductChoice> products, InvoiceDocument existing, String presetCustomerCode, SaveListener listener) {
        this.invoices = invoices;
        this.products = products;
        this.existing = existing;
        this.listener = listener;
        boolean editing = existing != null;
        dialog = new AppModalDialog(owner, editing ? "تعديل الفاتورة" : "فاتورة جديدة", editing ? "edit" : "file");

        List<CustomerChoice> customerOptions = new ArrayList<>();
        customerOptions.add(NO_CUSTOMER);
        customers.forEach(c -> customerOptions.add(new CustomerChoice(c)));
        if (editing && existing.customer() != null
                && customers.stream().noneMatch(c -> c.code().equals(existing.customer().code())))
            customerOptions.add(new CustomerChoice(existing.customer()));
        customer = new JComboBox<>(customerOptions.toArray(CustomerChoice[]::new));
        styleCombo(customer);

        // The form scrolls as a whole on small screens instead of squeezing the item table.
        WidthTrackingPanel content = new WidthTrackingPanel(
                new MigLayout("rtl,fillx,insets 0,gap 0 12,wrap 1", "[grow,fill]"));
        content.setBackground(Theme.CARD);
        content.add(invoiceSection(editing));
        content.add(linesSection());
        content.add(totalsSection());
        messages.setOpaque(false);
        content.add(messages);
        JScrollPane bodyScroll = new JScrollPane(content);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel body = dialog.body();
        body.setLayout(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        body.add(bodyScroll);

        dialog.addFooterButton(button("إلغاء", AppButton.Kind.SECONDARY, null, dialog::dispose));
        saveButtons.add(button(editing ? "حفظ التعديل" : "حفظ", AppButton.Kind.PRIMARY, null, () -> save(false)));
        saveButtons.forEach(dialog::addFooterButton);

        if (editing) {
            selectCustomer(existing.customer() == null ? null : existing.customer().code());
            date.setDate(existing.date());
            for (InvoiceDocumentLine line : existing.lines()) addLine(line);
            additionalDiscount.setText(InvoiceCalculator.money(existing.additionalDiscount()).toPlainString());
            cashPaid.setText(InvoiceCalculator.money(existing.cashPaid()).toPlainString());
        }
        if (!editing && presetCustomerCode != null) selectCustomer(presetCustomerCode);
        if (lineEditors.isEmpty()) addLine(null);

        customer.addActionListener(event -> {
            showCustomer();
            Customer selected = selectedCustomer();
            lineEditors.forEach(editor -> editor.loadProducts(selected));
            loadPreviousBalance();
        });
        date.addEditListener(this::loadPreviousBalance);
        additionalDiscount.getDocument().addDocumentListener(changes(this::recalculate));
        cashPaid.getDocument().addDocumentListener(changes(this::recalculate));
        showCustomer();
        loadPreviousBalance();
        recalculate();

        Rtl.apply(body);
        restoreAlignments();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(Math.min(1120, screen.width - 60), Math.min(800, screen.height - 60));
        dialog.setLocationRelativeTo(owner);
    }

    public void show() {
        dialog.setVisible(true);
    }

    // ----- sections ---------------------------------------------------------------------------

    private JPanel invoiceSection(boolean editing) {
        JPanel section = dialog.section("بيانات الفاتورة");
        JPanel grid = new JPanel(new MigLayout("rtl,fillx,insets 12,gap 12 8", "[grow,fill][200!,fill][220!,fill]"));
        grid.setBackground(Theme.CARD);
        JTextField number = numberField(editing ? existing.number() : NEW_NUMBER);
        number.setEditable(false);
        number.setBackground(new Color(0xF1, 0xF3, 0xF5));
        grid.add(field("العميل *", customer), "growx");
        grid.add(field("تاريخ البيان *", date), "growx");
        grid.add(field("رقم الفاتورة", number), "growx,wrap");
        grid.add(field("العنوان", address), "span 2,growx");
        grid.add(field("اسم الشركة", company), "growx");
        section.add(grid);
        return section;
    }

    private static final String LINE_COLUMNS = "[44!][grow,fill][95!][105!][110!][105!][115!][38!]";

    private JPanel linesSection() {
        JPanel section = dialog.section("الأصناف");
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBackground(Theme.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        JPanel header = new JPanel(new MigLayout("rtl,fillx,insets 6 0 6 0,gap 6 0", LINE_COLUMNS));
        header.setBackground(new Color(0x20, 0x37, 0x64));
        for (String text : new String[] {"مسلسل", "الصنف", "الكمية", "الوحدة (السعر)", "القيمة", "قيمة الخصم",
            "الصافي", ""}) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(Theme.font(Font.BOLD, 12));
            tableHeadings.add(label);
            header.add(label, "growx");
        }
        content.add(header, BorderLayout.NORTH);
        linesPanel.setBackground(Theme.CARD);
        JPanel holder = new WidthTrackingPanel(new BorderLayout());
        holder.setBackground(Theme.CARD);
        holder.add(linesPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setPreferredSize(new Dimension(1, 170));
        scroll.setMinimumSize(new Dimension(1, 130));
        content.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(button("إضافة صنف", AppButton.Kind.SECONDARY, "plus", () -> {
            addLine(null);
            recalculate();
        }));
        content.add(actions, BorderLayout.SOUTH);
        section.add(content);
        return section;
    }

    private JPanel totalsSection() {
        JPanel section = dialog.section("إجمالي الفاتورة");
        JPanel grid = new JPanel(new MigLayout("rtl,fillx,insets 10 12 10 12,gap 10 4",
                "[150!][grow,fill]40[180!][grow,fill]"));
        grid.setBackground(Theme.CARD);
        grid.add(caption("المدفوع نقداً"));
        grid.add(cashPaid);
        grid.add(caption("قيمة الفاتورة"));
        grid.add(invoiceValue, "wrap");
        grid.add(caption("إجمالي الكميات"));
        grid.add(totalQuantity);
        grid.add(caption("الرصيد السابق"));
        grid.add(previousBalance, "wrap");
        grid.add(caption("الرصيد النهائي"));
        grid.add(finalBalance);
        grid.add(caption("خصم إضافي"));
        grid.add(additionalDiscount, "wrap");
        grid.add(new JLabel(), "span 2");
        grid.add(caption("إجمالي الفاتورة والمديونية"));
        grid.add(totalWithDebt);
        finalBalance.setForeground(new Color(0xC0, 0x00, 0x00));
        section.add(grid);
        return section;
    }

    // ----- lines ------------------------------------------------------------------------------

    private void addLine(InvoiceDocumentLine initial) {
        LineEditor editor = new LineEditor(initial);
        lineEditors.add(editor);
        linesPanel.add(editor.row, "growx");
        renumber();
        Rtl.apply(editor.row);
        editor.centre();
        linesPanel.revalidate();
        linesPanel.repaint();
    }

    private void removeLine(LineEditor editor) {
        lineEditors.remove(editor);
        linesPanel.remove(editor.row);
        if (lineEditors.isEmpty()) addLine(null);
        renumber();
        linesPanel.revalidate();
        linesPanel.repaint();
        recalculate();
    }

    private void renumber() {
        for (int i = 0; i < lineEditors.size(); i++) lineEditors.get(i).serial.setText(String.valueOf(i + 1));
    }

    private final class LineEditor {
        final JPanel row = new JPanel(new MigLayout("rtl,fillx,insets 0,gap 6 0", LINE_COLUMNS));
        final JLabel serial = new JLabel("", SwingConstants.CENTER);
        final JComboBox<ProductChoice> product;
        final JTextField quantity = numberField("");
        final JTextField price = numberField("");
        final JTextField discount = numberField("");
        final JLabel value = lineAmount();
        final JLabel net = lineAmount();
        final String unitName;
        /** The product saved on an existing line; offered while the invoice's original customer is selected. */
        private final ProductChoice savedProduct;
        private boolean loading;

        LineEditor(InvoiceDocumentLine initial) {
            row.setBackground(Theme.CARD);
            serial.setFont(Theme.font(Font.BOLD, 12));
            InvoiceLine line = initial == null ? null : initial.line();
            savedProduct = line == null || line.productId() == null ? null
                    : products.stream().filter(p -> line.productId().equals(p.id())).findFirst()
                            .orElse(new ProductChoice(line.productId(), initial.itemName(), line.unitPrice(), null));
            product = new JComboBox<>();
            styleCombo(product);
            unitName = line == null ? "" : line.unitName();
            loadProducts(selectedCustomer());
            if (line != null) {
                select(line.productId());
                quantity.setText(line.quantity().stripTrailingZeros().toPlainString());
                price.setText(InvoiceCalculator.money(line.unitPrice()).toPlainString());
                discount.setText(line.discount().signum() == 0 ? "" : InvoiceCalculator.money(line.discount())
                        .toPlainString());
            } else {
                quantity.setText("1");
            }
            product.addActionListener(event -> {
                if (loading)
                    return;
                ProductChoice selected = (ProductChoice) product.getSelectedItem();
                if (selected != null && selected.id() != null)
                    price.setText(InvoiceCalculator.money(selected.price()).toPlainString());
                product.putClientProperty("JComponent.outline", null);
                recalculate();
            });
            DocumentListener changed = changes(InvoiceEditorDialog.this::recalculate);
            quantity.getDocument().addDocumentListener(changed);
            price.getDocument().addDocumentListener(changed);
            discount.getDocument().addDocumentListener(changed);
            JButton remove = new AppButton("", AppButton.Kind.DANGER, "trash");
            remove.setToolTipText("حذف الصنف");
            remove.addActionListener(event -> removeLine(this));
            row.add(serial, "growx");
            row.add(product, "growx");
            row.add(quantity, "growx");
            row.add(price, "growx");
            row.add(value, "growx");
            row.add(discount, "growx");
            row.add(net, "growx");
            row.add(remove, "align center");
        }

        /**
         * Shows only the selected customer's products. The current choice is kept when it belongs to
         * the customer (or is the line's saved, no-longer-listed product); otherwise the line is reset.
         */
        void loadProducts(Customer owner) {
            ProductChoice current = (ProductChoice) product.getSelectedItem();
            List<ProductChoice> options = new ArrayList<>();
            options.add(new ProductChoice(null, owner == null ? "اختر العميل أولاً" : "اختر الصنف", BigDecimal.ZERO,
                    null));
            if (owner != null)
                products.stream().filter(p -> owner.code().equals(p.customerCode())).forEach(options::add);
            boolean originalCustomer = owner != null && existing != null && existing.customer() != null
                    && owner.code().equals(existing.customer().code());
            if (savedProduct != null && originalCustomer && !options.contains(savedProduct))
                options.add(savedProduct);
            loading = true;
            try {
                product.setModel(new DefaultComboBoxModel<>(options.toArray(ProductChoice[]::new)));
                select(current == null ? null : current.id());
            } finally {
                loading = false;
            }
            recalculate();
        }

        private void select(Long productId) {
            boolean previous = loading;
            loading = true;
            try {
                product.setSelectedIndex(0);
                for (int i = 1; productId != null && i < product.getItemCount(); i++) {
                    if (productId.equals(product.getItemAt(i).id())) {
                        product.setSelectedIndex(i);
                        break;
                    }
                }
            } finally {
                loading = previous;
            }
        }

        /** Lenient line for live totals: unreadable numbers count as zero. */
        InvoiceLine preview() {
            ProductChoice selected = (ProductChoice) product.getSelectedItem();
            return new InvoiceLine(0, 0, selected == null ? null : selected.id(), lenient(quantity), unitName,
                    lenient(price), lenient(discount));
        }

        /** Strict line for saving, or null after marking the unreadable fields. */
        InvoiceLine read(List<String> problems, int number) {
            ProductChoice selected = (ProductChoice) product.getSelectedItem();
            BigDecimal q = strict(quantity, true, problems, "البند " + number + ": أدخل كمية صحيحة.");
            BigDecimal p = strict(price, true, problems, "البند " + number + ": أدخل سعر وحدة صحيحًا.");
            BigDecimal d = strict(discount, false, problems, "البند " + number + ": أدخل قيمة خصم صحيحة.");
            if (selected == null || selected.id() == null) product.putClientProperty("JComponent.outline", "error");
            if (q == null || p == null || d == null) return null;
            return new InvoiceLine(0, number, selected == null ? null : selected.id(), q, unitName, p, d);
        }

        void centre() {
            serial.setHorizontalAlignment(SwingConstants.CENTER);
            value.setHorizontalAlignment(SwingConstants.CENTER);
            net.setHorizontalAlignment(SwingConstants.CENTER);
            for (JTextField field : List.of(quantity, price, discount)) field.setHorizontalAlignment(SwingConstants.CENTER);
        }

        void update() {
            InvoiceLine line = preview();
            value.setText(format(InvoiceCalculator.lineValue(line)));
            net.setText(format(InvoiceCalculator.lineNet(line)));
        }
    }

    // ----- behaviour --------------------------------------------------------------------------

    private void selectCustomer(String code) {
        if (code == null) return;
        for (int i = 0; i < customer.getItemCount(); i++) {
            if (code.equals(customer.getItemAt(i).customer().code())) {
                customer.setSelectedIndex(i);
                return;
            }
        }
    }

    private Customer selectedCustomer() {
        CustomerChoice selected = (CustomerChoice) customer.getSelectedItem();
        return selected == null || selected.customer().code().isEmpty() ? null : selected.customer();
    }

    private void showCustomer() {
        Customer selected = selectedCustomer();
        address.setText(selected == null || selected.address() == null ? "—" : selected.address());
        company.setText(selected == null ? "—" : selected.name());
        customer.putClientProperty("JComponent.outline", null);
    }

    private LocalDate parsedDate() {
        return date.date();
    }

    /** Previous balance of the selected customer before this invoice, loaded off the Swing thread. */
    private void loadPreviousBalance() {
        Customer selected = selectedCustomer();
        LocalDate day = parsedDate();
        int request = ++balanceRequest;
        loadedPreviousBalance = null;
        if (selected == null || day == null) {
            previousBalance.setText(selected == null ? "اختر العميل" : "أدخل التاريخ");
            recalculate();
            return;
        }
        previousBalance.setText("جارٍ الحساب…");
        recalculate();
        Long invoiceId = existing == null ? null : existing.id();
        new SwingWorker<BigDecimal, Void>() {
            @Override
            protected BigDecimal doInBackground() throws Exception {
                return invoices.findPreviousBalance(selected.code(), day, invoiceId);
            }

            @Override
            protected void done() {
                if (request != balanceRequest) return;
                try {
                    loadedPreviousBalance = get();
                } catch (Exception exception) {
                    loadedPreviousBalance = null;
                    previousBalance.setText("تعذر الحساب");
                }
                recalculate();
            }
        }.execute();
    }

    private void recalculate() {
        List<InvoiceLine> lines = new ArrayList<>();
        for (LineEditor editor : lineEditors) {
            editor.update();
            lines.add(editor.preview());
        }
        BigDecimal previous = loadedPreviousBalance == null ? BigDecimal.ZERO : loadedPreviousBalance;
        InvoiceTotals totals = InvoiceCalculator.totals(lines, previous, lenient(additionalDiscount),
                lenient(cashPaid));
        totalQuantity.setText(formatQuantity(totals.totalQuantity()));
        invoiceValue.setText(format(totals.invoiceValue()));
        if (loadedPreviousBalance != null) previousBalance.setText(format(totals.previousBalance()));
        String pending = loadedPreviousBalance == null ? " *" : "";
        totalWithDebt.setText(format(totals.totalWithDebt()) + pending);
        finalBalance.setText(format(totals.finalBalance()) + pending);
    }

    private void save(boolean openPreview) {
        List<String> problems = new ArrayList<>();
        Customer selected = selectedCustomer();
        if (selected == null) customer.putClientProperty("JComponent.outline", "error");
        LocalDate day = parsedDate();
        if (day == null) {
            problems.add("اختر تاريخ البيان من التقويم أو اكتبه بصيغة yyyy-MM-dd.");
        }
        List<InvoiceLine> lines = new ArrayList<>();
        boolean readable = true;
        for (int i = 0; i < lineEditors.size(); i++) {
            InvoiceLine line = lineEditors.get(i).read(problems, i + 1);
            if (line == null) readable = false;
            else lines.add(line);
        }
        BigDecimal discount = strict(additionalDiscount, false, problems, "أدخل خصمًا إضافيًا صحيحًا.");
        BigDecimal cash = strict(cashPaid, false, problems, "أدخل المدفوع نقدًا بشكل صحيح.");
        InvoiceInput input = new InvoiceInput(selected == null ? "" : selected.code(), day, lines, discount, cash);
        if (readable && discount != null && cash != null) {
            for (String problem : validation.validate(input)) {
                if (day == null && problem.contains("تاريخ")) continue;
                problems.add(problem);
            }
        } else if (selected == null) {
            problems.addFirst("اختر العميل.");
        }
        if (!problems.isEmpty()) {
            showMessages(problems);
            return;
        }
        showMessages(List.of());
        saveButtons.forEach(button -> button.setEnabled(false));
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                if (existing == null) {
                    InvoiceSummary created = invoices.create(input);
                    return created.id();
                }
                invoices.update(existing.id(), input);
                return existing.id();
            }

            @Override
            protected void done() {
                saveButtons.forEach(button -> button.setEnabled(true));
                try {
                    long id = get();
                    dialog.dispose();
                    listener.saved(id, openPreview);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof InvoiceValidationException invalid) showMessages(invalid.problems());
                    else showMessages(List.of("تعذر حفظ الفاتورة: " + (cause == null ? exception : cause).getMessage()));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showMessages(List<String> problems) {
        messages.removeAll();
        for (String problem : problems) {
            JLabel label = new JLabel("• " + problem, SwingConstants.RIGHT);
            label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            label.setForeground(Theme.ERROR);
            label.setFont(Theme.font(Font.PLAIN, 12));
            messages.add(label, "growx");
        }
        messages.revalidate();
        messages.repaint();
        dialog.getContentPane().revalidate();
    }

    // ----- parsing and widgets ------------------------------------------------------------------

    /** Accepts Western or Arabic-Indic digits and thousands separators. */
    private static String normalise(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.trim().toCharArray()) {
            if (c >= '٠' && c <= '٩') out.append((char) ('0' + c - '٠'));
            else if (c >= '۰' && c <= '۹') out.append((char) ('0' + c - '۰'));
            else if (c == '٫') out.append('.');
            else if (c == ',' || c == '٬' || c == ' ') continue;
            else out.append(c);
        }
        return out.toString();
    }

    private static BigDecimal lenient(JTextField field) {
        try {
            String text = normalise(field.getText());
            return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal strict(JTextField field, boolean required, List<String> problems, String message) {
        String text = normalise(field.getText());
        if (text.isEmpty() && !required) {
            field.putClientProperty("JComponent.outline", null);
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(text);
            field.putClientProperty("JComponent.outline", null);
            return value;
        } catch (NumberFormatException exception) {
            field.putClientProperty("JComponent.outline", "error");
            problems.add(message);
            return null;
        }
    }

    private static String format(BigDecimal value) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US))
                .format(InvoiceCalculator.money(value));
    }

    private static String formatQuantity(BigDecimal value) {
        if (value.stripTrailingZeros().scale() <= 0)
            return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
        return format(value);
    }

    private static DocumentListener changes(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private static JTextField numberField(String value) {
        JTextField field = new JTextField(value);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(Theme.font(Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(1, Theme.FIELD_HEIGHT));
        return field;
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.font(Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        combo.setMinimumSize(new Dimension(1, Theme.FIELD_HEIGHT));
        combo.setMaximumRowCount(14);
    }

    private static JLabel valueLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.PLAIN, 12));
        label.setForeground(Theme.TEXT);
        label.setOpaque(true);
        label.setBackground(new Color(0xF6, 0xF8, 0xFA));
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        label.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        return label;
    }

    private static JLabel amountLabel() {
        JLabel label = valueLabel("0.00");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, 13));
        return label;
    }

    private static JLabel lineAmount() {
        JLabel label = amountLabel();
        label.setFont(Theme.font(Font.BOLD, 12));
        return label;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, 12));
        label.setForeground(Theme.TEXT);
        label.setOpaque(true);
        label.setBackground(new Color(0xD9, 0xD9, 0xD9));
        label.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        return label;
    }

    private static JPanel field(String title, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel label = new JLabel(title, FieldIcons.iconFor(title, 14), SwingConstants.RIGHT);
        label.setIconTextGap(6);
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        label.setFont(Theme.font(Font.BOLD, 11));
        label.setForeground(Theme.LABEL_TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input);
        return panel;
    }

    private static JButton button(String text, AppButton.Kind kind, String icon, Runnable action) {
        JButton button = icon == null ? new AppButton(text, kind) : new AppButton(text, kind, icon);
        button.addActionListener(event -> action.run());
        return fitText(button);
    }

    /** AppButton has a fixed 110px width; widen it when its Arabic label needs more room. */
    static JButton fitText(JButton button) {
        Dimension fixed = button.getPreferredSize();
        button.setPreferredSize(null);
        int natural = button.getPreferredSize().width + 16;
        button.setPreferredSize(new Dimension(Math.max(fixed.width, natural), fixed.height));
        return button;
    }

    /** Rtl.apply right-aligns every label and field; amounts and the item table read better centred. */
    private void restoreAlignments() {
        for (JLabel label : List.of(totalQuantity, invoiceValue, previousBalance, totalWithDebt, finalBalance))
            label.setHorizontalAlignment(SwingConstants.CENTER);
        for (JTextField field : List.of(additionalDiscount, cashPaid))
            field.setHorizontalAlignment(SwingConstants.CENTER);
        for (LineEditor editor : lineEditors) editor.centre();
        tableHeadings.forEach(label -> label.setHorizontalAlignment(SwingConstants.CENTER));
    }

    /** Panel that keeps the width of the scroll pane it is shown in, so table columns can grow. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager layout) {
            super(layout);
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
