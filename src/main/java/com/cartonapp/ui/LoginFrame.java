package com.cartonapp.ui;

import com.cartonapp.service.AuthenticationService;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** First application screen: password-only login. */
public final class LoginFrame extends JFrame {
    private final AuthenticationService authenticationService;
    private final JPasswordField passwordField = new JPasswordField();
    private final PrimaryButton loginButton = new PrimaryButton("فتح البرنامج");
    private final JLabel feedbackLabel = new JLabel("", SwingConstants.CENTER);

    public LoginFrame(AuthenticationService authenticationService) {
        super("الليثي باك");
        this.authenticationService = authenticationService;
        setUndecorated(true);
        setIconImages(BrandLogo.windowIcons());
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1064, 600);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(createLoginTitleBar(), BorderLayout.NORTH);
        root.add(createContent(), BorderLayout.CENTER);
        setContentPane(root);
        feedbackLabel.setVisible(false);
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(this::login);
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { clearValidation(); }
            @Override public void removeUpdate(DocumentEvent event) { clearValidation(); }
            @Override public void changedUpdate(DocumentEvent event) { clearValidation(); }
        });
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.add(createBrandPanel(), BorderLayout.EAST);
        JPanel canvas = new JPanel(new GridBagLayout());
        canvas.setBackground(Theme.CANVAS);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        canvas.add(createCard(), constraints);
        content.add(canvas, BorderLayout.CENTER);
        return content;
    }

    /** Navy brand side: logo, factory name and what the program does. */
    private JPanel createBrandPanel() {
        float[] phase = {0f};
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                double a = phase[0];
                g.setPaint(new GradientPaint(0, (float) (Math.sin(a) * 80), Theme.NAVY_3, getWidth(), getHeight(), Theme.NAVY));
                g.fillRect(0, 0, getWidth(), getHeight());
                // slowly drifting glow circles
                g.setColor(new Color(0x8C, 0xC8, 0xF5, 26));
                g.fillOval((int) (getWidth() - 220 + Math.sin(a) * 30), (int) (-120 + Math.cos(a) * 20), 360, 360);
                g.fillOval((int) (-140 + Math.cos(a * 0.8) * 30), (int) (getHeight() - 180 + Math.sin(a * 0.8) * 20), 300, 300);
                g.setColor(new Color(0x1E, 0x6F, 0xD9, 30));
                g.fillOval((int) (getWidth() / 2 - 90 + Math.sin(a * 1.3) * 60), (int) (getHeight() / 2 + Math.cos(a) * 40), 180, 180);
                g.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(470, super.getPreferredSize().height);
            }
        };
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 18, 0);
        JLabel mark = new JLabel(BrandLogo.mark(92));
        mark.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(mark, c);
        JLabel name = new JLabel(BrandLogo.NAME, SwingConstants.RIGHT);
        name.setFont(Theme.font(Font.BOLD, 34));
        name.setForeground(Color.WHITE);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 4, 0);
        panel.add(name, c);
        JLabel tagline = new JLabel(BrandLogo.TAGLINE + " — نظام الحسابات", SwingConstants.RIGHT);
        tagline.setFont(Theme.font(Font.PLAIN, 14));
        tagline.setForeground(Theme.BABY);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 30, 0);
        panel.add(tagline, c);
        String[][] features = {
            {"users", "العملاء والمديونيات وكشوف الحساب"},
            {"file", "فواتير المبيعات بنفس قالب الشركة"},
            {"box", "المنتجات وأسعار الكرتون"},
            {"download", "تقارير PDF و Excel"}};
        for (int i = 0; i < features.length; i++) {
            JLabel feature = new JLabel(features[i][1], Icons.icon(features[i][0], 18, Theme.BABY), SwingConstants.RIGHT);
            feature.setFont(Theme.font(Font.PLAIN, 13));
            feature.setForeground(new Color(0xDD, 0xEB, 0xF8));
            feature.setIconTextGap(12);
            feature.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            c.gridy = 3 + i;
            c.insets = new Insets(0, 0, 14, 0);
            panel.add(feature, c);
        }
        panel.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 56));
        // gentle motion: background drifts and the logo floats up and down
        javax.swing.Timer motion = new javax.swing.Timer(40, e -> {
            phase[0] += 0.03f;
            mark.setBorder(BorderFactory.createEmptyBorder((int) (5 + Math.sin(phase[0] * 2) * 5), 0,
                    (int) (5 - Math.sin(phase[0] * 2) * 5), 0));
            panel.repaint();
        });
        motion.start();
        panel.addHierarchyListener(e -> { if (!panel.isDisplayable()) motion.stop(); });
        return panel;
    }

    private JPanel createLoginTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(1, 34));
        titleBar.setBackground(Theme.NAVY);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.NAVY_2));
        JLabel name = new JLabel("الليثي باك", BrandLogo.mark(18), SwingConstants.RIGHT);
        name.setIconTextGap(8);
        name.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        name.setFont(Theme.font(Font.BOLD, 11));
        name.setForeground(Color.WHITE);
        name.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        titleBar.add(name, BorderLayout.EAST);

        JPanel controls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        controls.setBackground(Theme.NAVY);
        controls.add(windowButton("—", () -> setState(JFrame.ICONIFIED)));
        controls.add(windowButton("□", this::toggleMaximize));
        controls.add(windowButton("×", this::dispose));
        titleBar.add(controls, BorderLayout.WEST);

        MouseAdapter drag = new MouseAdapter() {
            private Point start;
            @Override public void mousePressed(MouseEvent event) { start = event.getPoint(); }
            @Override public void mouseDragged(MouseEvent event) {
                Point location = getLocation();
                setLocation(location.x + event.getX() - start.x, location.y + event.getY() - start.y);
            }
        };
        titleBar.addMouseListener(drag);
        titleBar.addMouseMotionListener(drag);
        return titleBar;
    }

    private JButton windowButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(46, 33));
        button.setFont(Theme.font(Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void toggleMaximize() {
        setExtendedState((getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH
                ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
    }

    private JPanel createCard() {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.CARD_RADIUS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferred = super.getPreferredSize();
                return new Dimension(Theme.CARD_WIDTH, preferred.height);
            }
        };
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.CARD_PADDING_TOP,
                        Theme.CARD_PADDING_HORIZONTAL, Theme.CARD_PADDING_BOTTOM,
                        Theme.CARD_PADDING_HORIZONTAL)));
        card.putClientProperty("JComponent.arc", Theme.CARD_RADIUS);
        card.putClientProperty("JComponent.shadow", "small");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel logo = new JLabel(BrandLogo.mark(Theme.LOGO_SIZE));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 0;
        c.insets = new Insets(0, 0, 13, 0);
        card.add(logo, c);

        JLabel title = new JLabel("تسجيل الدخول", SwingConstants.CENTER);
        title.setForeground(Theme.NAVY);
        title.setFont(Theme.font(Font.BOLD, 22));
        c.gridy++;
        c.insets = new Insets(0, 0, 3, 0);
        card.add(title, c);

        JLabel subtitle = new JLabel("نظام إدارة حسابات مصنع الكرتون", SwingConstants.CENTER);
        subtitle.setForeground(Theme.MUTED_TEXT);
        subtitle.setFont(Theme.font(Font.PLAIN, 12));
        c.gridy++;
        c.insets = new Insets(0, 0, 24, 0);
        card.add(subtitle, c);

        JLabel passwordLabel = new JLabel("كلمة المرور");
        passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        passwordLabel.setForeground(Theme.LABEL_TEXT);
        passwordLabel.setFont(Theme.font(Font.BOLD, 11));
        c.gridy++;
        c.insets = new Insets(0, 0, 5, 0);
        card.add(passwordLabel, c);

        passwordField.setHorizontalAlignment(SwingConstants.RIGHT);
        passwordField.setFont(Theme.font(Font.PLAIN, 13));
        passwordField.putClientProperty("JTextField.placeholderText", "أدخل كلمة المرور");
        passwordField.setPreferredSize(new Dimension(1, Theme.FIELD_HEIGHT));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.FIELD_BORDER),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        passwordField.putClientProperty("JTextField.leadingIcon", loadIcon("icons/lock.svg", 18, 18));
        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(passwordField, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(loginButton, c);

        feedbackLabel.setForeground(Theme.ERROR);
        feedbackLabel.setFont(Theme.font(Font.PLAIN, 9));
        c.gridy++;
        c.insets = new Insets(0, 0, 10, 0);
        card.add(feedbackLabel, c);

        JLabel note = new JLabel("برنامج محلي آمن • نسخة المستخدم الواحد", SwingConstants.CENTER);
        note.setForeground(Theme.NOTE_TEXT);
        note.setFont(Theme.font(Font.PLAIN, 10));
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(note, c);
        return card;
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        return new FlatSVGIcon(path, width, height);
    }

    private void clearValidation() {
        if (feedbackLabel.isVisible()) {
            feedbackLabel.setText(" ");
            feedbackLabel.setVisible(false);
            applyPasswordBorder(Theme.FIELD_BORDER, false);
        }
    }

    private void applyPasswordBorder(Color color, boolean error) {
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        passwordField.putClientProperty("JComponent.outline", error ? "error" : null);
    }

    private void login(ActionEvent event) {
        char[] password = passwordField.getPassword();
        loginButton.setEnabled(false);
        feedbackLabel.setVisible(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return authenticationService.authenticate(password);
            }

            @Override
            protected void done() {
                java.util.Arrays.fill(password, '\0');
                try {
                    if (get()) {
                        dispose();
                        SplashWindow.show(() -> new MainFrame().setVisible(true));
                    } else {
                        feedbackLabel.setText("كلمة المرور غير صحيحة");
                        feedbackLabel.setVisible(true);
                        applyPasswordBorder(new Color(0xC7, 0x3C, 0x44), true);
                        passwordField.requestFocusInWindow();
                        loginButton.setEnabled(true);
                    }
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    feedbackLabel.setText(cause instanceof IllegalStateException && cause.getMessage() != null
                            ? cause.getMessage() : "تعذر تسجيل الدخول. حاول مرة أخرى.");
                    feedbackLabel.setVisible(true);
                    applyPasswordBorder(new Color(0xC7, 0x3C, 0x44), true);
                    loginButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
