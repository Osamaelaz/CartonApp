package com.cartonapp.ui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;

/** Secondary tabs drawn as pill buttons with an icon and a count badge; the active one is a blue gradient. */
public final class SubTabBar extends JPanel {
    private final Map<String, Pill> pills = new LinkedHashMap<>();
    private final Consumer<String> onSelection;

    public SubTabBar(Consumer<String> onSelection) {
        super(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        this.onSelection = onSelection;
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setOpaque(false);
    }

    public void addTab(String id, String text, String count) {
        addTab(id, text, count, switch (id) {
            case "statement" -> "receipt";
            case "products" -> "box";
            case "invoices" -> "file";
            default -> "chart";
        });
    }

    public void addTab(String id, String text, String count, String icon) {
        Pill pill = new Pill(text, count, icon);
        pill.addActionListener(e -> {
            select(id);
            onSelection.accept(id);
        });
        pills.put(id, pill);
        add(pill);
    }

    public void select(String id) {
        pills.forEach((key, pill) -> pill.setActive(key.equals(id)));
    }

    private static final class Pill extends JButton {
        private final String count;
        private final String iconName;
        private boolean active;

        Pill(String text, String count, String iconName) {
            super(text);
            this.count = count;
            this.iconName = iconName;
            setFont(Theme.font(Font.BOLD, 13));
            setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            setHorizontalTextPosition(TRAILING);
            setIconTextGap(8);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // the count badge sits at the left end, so the left padding makes room for it
            setBorder(BorderFactory.createEmptyBorder(9, count == null ? 18 : 48, 9, 18));
            getModel().addChangeListener(e -> repaint());
            setActive(false);
        }

        void setActive(boolean on) {
            active = on;
            setForeground(on ? Color.WHITE : Theme.NAVY);
            setIcon(Icons.icon(iconName, 17, on ? Color.WHITE : Theme.PRIMARY));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight() - 3;
            boolean hover = getModel().isRollover();
            if (active) {
                g.setColor(new Color(0x1E, 0x6F, 0xD9, 60));
                g.fillRoundRect(1, 3, w - 2, h, h, h);
                g.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 0, h, Theme.NAVY_3));
            } else {
                g.setColor(new Color(0x0A, 0x25, 0x40, 16));
                g.fillRoundRect(1, 3, w - 2, h, h, h);
                g.setColor(hover ? Theme.BABY_LIGHT : Color.WHITE);
            }
            g.fillRoundRect(0, 0, w, h, h, h);
            if (!active) {
                g.setColor(hover ? Theme.BABY : Theme.BORDER);
                g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            }
            if (count != null) {
                // count badge at the left end of the pill
                Font font = Theme.font(Font.BOLD, 11);
                g.setFont(font);
                FontMetrics m = g.getFontMetrics();
                int bw = Math.max(22, m.stringWidth(count) + 12), bh = 20;
                int bx = 12, by = (h - bh) / 2;
                g.setColor(active ? new Color(255, 255, 255, 60) : Theme.BABY_LIGHT);
                g.fillRoundRect(bx, by, bw, bh, bh, bh);
                g.setColor(active ? Color.WHITE : Theme.PRIMARY_DARK);
                g.drawString(count, bx + (bw - m.stringWidth(count)) / 2, by + (bh + m.getAscent() - m.getDescent()) / 2);
            }
            g.dispose();
            Graphics2D text = (Graphics2D) graphics.create();
            text.translate(0, -1);
            super.paintComponent(text);
            text.dispose();
        }
    }
}
