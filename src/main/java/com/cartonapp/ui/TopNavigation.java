package com.cartonapp.ui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;

/** Navy application bar: brand on the right, section tabs, settings and the user chip on the left. */
public final class TopNavigation extends JPanel {
    private final Map<String, JButton> tabs = new LinkedHashMap<>();
    private final Map<String, String> tabIcons = new LinkedHashMap<>();
    private final Consumer<String> selection;
    private String active;

    public TopNavigation(Consumer<String> selection) {
        super(new BorderLayout());
        this.selection = selection;
        setBackground(Theme.NAVY);
        setPreferredSize(new Dimension(1, 66));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Theme.PRIMARY),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)));
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 12));
        right.setOpaque(false);
        // FlowLayout lays children left-to-right: add in reverse so the brand sits at the right edge.
        right.add(tab("invoices", "الفواتير", "file"));
        right.add(tab("products", "المنتجات", "box"));
        right.add(tab("customers", "العملاء", "users"));
        right.add(Box.createHorizontalStrut(26));
        right.add(new BrandLogo(40, true));
        add(right, BorderLayout.EAST);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        left.setOpaque(false);
        JLabel user = new JLabel("المحاسب", Icons.icon("user-circle", 20, Theme.BABY), SwingConstants.RIGHT);
        user.setFont(Theme.font(Font.BOLD, 12));
        user.setForeground(Color.WHITE);
        user.setIconTextGap(7);
        user.setOpaque(true);
        user.setBackground(Theme.NAVY_2);
        user.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        user.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        left.add(user);
        left.add(tab("settings", "الإعدادات", "gear"));
        add(left, BorderLayout.WEST);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(getWidth(), 0, Theme.NAVY, 0, 0, Theme.NAVY_2));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    private JButton tab(String id, String title, String icon) {
        JButton b = new JButton(title, Icons.icon(icon, 18, Theme.NAV_TEXT));
        b.setName(id);
        b.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        b.setHorizontalTextPosition(JButton.TRAILING);
        b.setIconTextGap(8);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setForeground(Theme.NAV_TEXT);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(Theme.NAVY_2);
        b.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        boolean[] over = {false};
        b.getModel().addChangeListener(event -> {
            boolean rollover = b.getModel().isRollover();
            if (id.equals(active) || rollover == over[0]) return;
            over[0] = rollover;
            // Inactive tabs are transparent on the gradient bar; hover fades a navy pill in and out.
            b.setOpaque(true);
            Color from = rollover ? Theme.NAVY : Theme.NAVY_2;
            Color to = rollover ? Theme.NAVY_2 : Theme.NAVY;
            Animator.animate(160, t -> { if (!id.equals(active)) b.setBackground(Animator.blend(from, to, t)); },
                    () -> { if (!id.equals(active) && !rollover) b.setOpaque(false); });
        });
        b.addActionListener(e -> {
            select(id);
            selection.accept(id);
        });
        tabs.put(id, b);
        tabIcons.put(id, icon);
        return b;
    }

    public void select(String id) {
        active = id;
        tabs.forEach((key, button) -> {
            boolean on = key.equals(id);
            Color foreground = on ? Color.WHITE : Theme.NAV_TEXT;
            button.setForeground(foreground);
            button.setIcon(Icons.icon(tabIcons.get(key), 18, on ? Theme.BABY : Theme.NAV_TEXT));
            button.setOpaque(on);
            if (on)
                Animator.animate(260, t -> button.setBackground(Animator.blend(Theme.NAVY, Theme.NAVY_3, t)), null);
            else
                button.setBackground(Theme.NAVY_2);
            button.setBorder(on
                    ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.BABY),
                            BorderFactory.createEmptyBorder(9, 16, 7, 16))
                    : BorderFactory.createEmptyBorder(9, 16, 9, 16));
        });
    }
}
