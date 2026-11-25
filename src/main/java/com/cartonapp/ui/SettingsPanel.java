package com.cartonapp.ui;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.config.DatabaseConfig;
import com.cartonapp.service.ProductPricingService;
import com.cartonapp.service.SettingsService;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

/**
 * Settings screen: company details (printed on invoices and reports), password change, the
 * pricing constants in use (read only) and system information.
 */
final class SettingsPanel extends JPanel {
    private final SettingsService settings;
    private final Toast toast;
    private final Consumer<CompanyProfile> onCompanySaved;

    private final JTextField companyName = field();
    private final JTextField companyAddress = field();
    private final JTextField companyPhone = field();
    private final JLabel companyStatus = status();
    private final JPasswordField currentPassword = password();
    private final JPasswordField newPassword = password();
    private final JPasswordField confirmPassword = password();
    private final JPanel passwordMessages = new JPanel(new MigLayout("rtl,insets 0,gap 0 2,wrap 1", "[grow,fill]"));
    private final JLabel defaultPasswordWarning = status();
    private final JLabel databaseStatus = status();

    SettingsPanel(SettingsService settings, Toast toast, Consumer<CompanyProfile> onCompanySaved) {
        // Two equal columns regardless of text length (long notes wrap inside their card).
        super(new GridLayout(2, 2, 13, 13));
        this.settings = settings;
        this.toast = toast;
        this.onCompanySaved = onCompanySaved;
        setOpaque(false);
        add(companyCard());
        add(passwordCard());
        add(pricingCard());
        add(systemCard());
        Rtl.apply(this);
        load();
    }

    /**
     * Report a modest width so the page fits the window (long notes then wrap) instead of widening
     * the page by the length of the unwrapped text.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.min(size.width, 900), size.height + 40);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, super.getMinimumSize().height);
    }

    // ----- cards ------------------------------------------------------------------------------

    private JPanel companyCard() {
        JPanel body = body();
        body.add(note("تظهر هذه البيانات في رأس كل فاتورة وفي تقارير PDF و Excel."), "span 2");
        body.add(labeled("اسم الشركة *", companyName), "span 2");
        body.add(labeled("العنوان", companyAddress));
        body.add(labeled("الهاتف", companyPhone));
        body.add(companyStatus, "span 2");
        body.add(actions(button("حفظ بيانات الشركة", AppButton.Kind.PRIMARY, "file", this::saveCompany)), "span 2");
        return card("بيانات الشركة", "users", body);
    }

    private JPanel passwordCard() {
        JPanel body = body();
        defaultPasswordWarning.setForeground(new Color(0xB0, 0x5A, 0x00));
        body.add(defaultPasswordWarning, "span 2");
        body.add(labeled("كلمة المرور الحالية", currentPassword), "span 2");
        body.add(labeled("كلمة المرور الجديدة", newPassword));
        body.add(labeled("تأكيد كلمة المرور الجديدة", confirmPassword));
        passwordMessages.setOpaque(false);
        body.add(passwordMessages, "span 2");
        body.add(note("كلمة المرور تُحفظ مشفّرة (Hash) ولا يمكن قراءتها من قاعدة البيانات. 4 أحرف على الأقل."), "span 2");
        body.add(actions(button("تغيير كلمة المرور", AppButton.Kind.PRIMARY, "lock", this::changePassword)), "span 2");
        return card("كلمة المرور", "lock", body);
    }

    private JPanel pricingCard() {
        JPanel body = body();
        body.add(note("يُحسب سعر الكرتونة تلقائيًا: مساحة الشيت × (متوسط سعر الطبقات + المصنعية للطن) × معامل التعريج × الجرام."
                + " طول الشيت = (الطول + العرض) × 2، وعرض الشيت = العرض + الارتفاع."), "span 2");
        for (String[] constant : ProductPricingService.constants()) {
            body.add(caption(constant[0]));
            body.add(value(constant[1]));
        }
        body.add(note("هذه القيم للعرض فقط. تعديلها يحتاج اعتماد معادلة التسعير أولًا."), "span 2");
        return card("معادلة التسعير", "box", body);
    }

    private JPanel systemCard() {
        JPanel body = body();
        String database = "—";
        String user = "—";
        try {
            DatabaseConfig config = DatabaseConfig.loadDefault();
            database = config.url().replaceFirst("^jdbc:postgresql://", "");
            user = config.user();
        } catch (Exception ignored) {
            // shown as "—"; the connection test reports the problem
        }
        body.add(caption("قاعدة البيانات"));
        body.add(value(database));
        body.add(caption("المستخدم"));
        body.add(value(user));
        body.add(caption("حالة الاتصال"));
        body.add(databaseStatus);
        Path exports = com.cartonapp.config.AppPaths.exportsDir().toAbsolutePath();
        body.add(caption("مجلد التصدير"));
        body.add(value(exports.toString()));
        body.add(caption("الإصدار"));
        body.add(value("1.0"));
        body.add(actions(button("اختبار الاتصال", AppButton.Kind.SECONDARY, "gear", this::testConnection),
                button("فتح مجلد التصدير", AppButton.Kind.SECONDARY, "download", () -> openFolder(exports))), "span 2");
        return card("النظام", "gear", body);
    }

    // ----- behaviour --------------------------------------------------------------------------

    private void load() {
        new SwingWorker<Object[], Void>() {
            protected Object[] doInBackground() throws Exception {
                return new Object[] {settings.company(), settings.usesDefaultPassword(), settings.databaseVersion()};
            }

            protected void done() {
                try {
                    Object[] values = get();
                    CompanyProfile company = (CompanyProfile) values[0];
                    companyName.setText(company.name());
                    companyAddress.setText(company.address());
                    companyPhone.setText(company.phone());
                    defaultPasswordWarning.setText((Boolean) values[1]
                            ? "تنبيه: ما زالت كلمة المرور الافتراضية مستخدمة. يُنصح بتغييرها." : " ");
                    connected(String.valueOf(values[2]));
                } catch (Exception exception) {
                    disconnected(exception);
                }
            }
        }.execute();
    }

    private void saveCompany() {
        CompanyProfile company = new CompanyProfile(companyName.getText(), companyAddress.getText(), companyPhone.getText());
        if (company.name().isBlank()) {
            companyName.putClientProperty("JComponent.outline", "error");
            companyStatus.setForeground(Theme.ERROR);
            companyStatus.setText("اسم الشركة مطلوب.");
            return;
        }
        companyName.putClientProperty("JComponent.outline", null);
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                settings.saveCompany(company);
                return null;
            }

            protected void done() {
                try {
                    get();
                    companyStatus.setForeground(new Color(0x17, 0x82, 0x49));
                    companyStatus.setText("تم الحفظ. ستظهر البيانات الجديدة في الفواتير والتقارير.");
                    onCompanySaved.accept(company);
                    toast.showMessage("تم حفظ بيانات الشركة");
                } catch (Exception exception) {
                    companyStatus.setForeground(Theme.ERROR);
                    companyStatus.setText("تعذر الحفظ: " + root(exception));
                }
            }
        }.execute();
    }

    private void changePassword() {
        char[] current = currentPassword.getPassword();
        char[] next = newPassword.getPassword();
        char[] confirm = confirmPassword.getPassword();
        new SwingWorker<List<String>, Void>() {
            protected List<String> doInBackground() throws Exception {
                return settings.changePassword(current, next, confirm);
            }

            protected void done() {
                java.util.Arrays.fill(current, '\0');
                java.util.Arrays.fill(next, '\0');
                java.util.Arrays.fill(confirm, '\0');
                try {
                    List<String> problems = get();
                    if (problems.isEmpty()) {
                        currentPassword.setText("");
                        newPassword.setText("");
                        confirmPassword.setText("");
                        defaultPasswordWarning.setText(" ");
                        showPasswordMessages(List.of("تم تغيير كلمة المرور. استخدمها عند تسجيل الدخول القادم."), false);
                        toast.showMessage("تم تغيير كلمة المرور");
                    } else {
                        showPasswordMessages(problems, true);
                    }
                } catch (Exception exception) {
                    showPasswordMessages(List.of("تعذر تغيير كلمة المرور: " + root(exception)), true);
                }
            }
        }.execute();
    }

    private void showPasswordMessages(List<String> messages, boolean error) {
        passwordMessages.removeAll();
        for (String message : messages) {
            JLabel label = status();
            label.setForeground(error ? Theme.ERROR : new Color(0x17, 0x82, 0x49));
            label.setText((error ? "• " : "✓ ") + message);
            passwordMessages.add(label, "growx");
        }
        passwordMessages.revalidate();
        passwordMessages.repaint();
    }

    private void testConnection() {
        databaseStatus.setForeground(Theme.MUTED_TEXT);
        databaseStatus.setText("جارٍ الاختبار…");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                return settings.databaseVersion();
            }

            protected void done() {
                try {
                    connected(get());
                } catch (Exception exception) {
                    disconnected(exception);
                }
            }
        }.execute();
    }

    private void connected(String version) {
        databaseStatus.setForeground(new Color(0x17, 0x82, 0x49));
        databaseStatus.setText("متصل — " + version);
    }

    private void disconnected(Exception exception) {
        databaseStatus.setForeground(Theme.ERROR);
        databaseStatus.setText("غير متصل: " + root(exception));
    }

    private void openFolder(Path folder) {
        try {
            Files.createDirectories(folder);
            Desktop.getDesktop().open(folder.toFile());
        } catch (Exception exception) {
            toast.showMessage("تعذر فتح المجلد: " + root(exception));
        }
    }

    private static String root(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    // ----- widgets ----------------------------------------------------------------------------

    private static JPanel card(String title, String icon, JPanel body) {
        AppCard card = new AppCard();
        JLabel heading = new JLabel(title, Icons.icon(icon, 18, Theme.PRIMARY), SwingConstants.RIGHT);
        heading.setFont(Theme.font(Font.BOLD, 14));
        heading.setForeground(Theme.TEXT);
        heading.setIconTextGap(8);
        heading.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(13, 15, 13, 15)));
        card.add(heading, BorderLayout.NORTH);
        card.add(body);
        return card;
    }

    private static JPanel body() {
        JPanel body = new JPanel(new MigLayout("rtl,fillx,insets 15,gap 12 10,wrap 2", "[grow,fill,sg c,10:][grow,fill,sg c,10:]"));
        body.setBackground(Theme.CARD);
        return body;
    }

    private static JPanel labeled(String title, JComponent input) {
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

    private static JTextField field() {
        JTextField field = new JTextField();
        field.setFont(Theme.font(Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        return field;
    }

    private static JPasswordField password() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.font(Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        field.putClientProperty("JPasswordField.showRevealButton", true);
        return field;
    }

    private static JLabel status() {
        JLabel label = new JLabel(" ", SwingConstants.RIGHT) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, super.getPreferredSize().height);
            }
        };
        label.setFont(Theme.font(Font.PLAIN, 11));
        label.setForeground(Theme.MUTED_TEXT);
        return label;
    }

    /** Wrapping, read-only note that takes the card's width instead of widening it. */
    private static JTextArea note(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(Theme.font(Font.PLAIN, 11));
        area.setForeground(Theme.MUTED_TEXT);
        area.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        area.setColumns(1);
        return area;
    }

    private static JPanel actions(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        for (JButton button : buttons) panel.add(button);
        return panel;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.PLAIN, 12));
        label.setForeground(Theme.MUTED_TEXT);
        return label;
    }

    private static JLabel value(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.BOLD, 12));
        label.setForeground(Theme.TEXT);
        label.setToolTipText(text);
        // Long values (paths) are cut with "…" instead of widening the card; the tooltip shows them.
        label.setPreferredSize(new Dimension(40, 22));
        label.setMinimumSize(new Dimension(10, 22));
        return label;
    }

    private static JButton button(String text, AppButton.Kind kind, String icon, Runnable action) {
        JButton button = new AppButton(text, kind, icon);
        button.addActionListener(event -> action.run());
        return InvoiceEditorDialog.fitText(button);
    }
}
