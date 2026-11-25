package com.cartonapp.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** Key-figure card: tinted icon badge, caption and a large value, with a thin accent strip. */
public final class MetricCard extends AppCard {
    public enum Tone { NEUTRAL, BLUE, GREEN, ORANGE }

    public MetricCard(String title, String value, Tone tone) {
        this(title, value, tone, defaultIcon(tone), null);
    }

    public MetricCard(String title, String value, Tone tone, String icon) {
        this(title, value, tone, icon, null);
    }

    public MetricCard(String title, String value, Tone tone, JComponent note) {
        this(title, value, tone, defaultIcon(tone), note);
    }

    public MetricCard(String title, String value, Tone tone, String icon, JComponent note) {
        Color accent = accent(tone);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createMatteBorder(0, 0, 0, 5, accent)));
        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        body.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        body.add(new Badge(icon, accent, tint(tone)), BorderLayout.EAST);
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        JLabel heading = new JLabel(title, SwingConstants.RIGHT);
        heading.setFont(Theme.font(Font.BOLD, 11));
        heading.setForeground(Theme.MUTED_TEXT);
        text.add(heading, BorderLayout.NORTH);
        JLabel amount = new JLabel(value, SwingConstants.RIGHT);
        countUp(amount, value);
        amount.setFont(Theme.font(Font.BOLD, 21));
        amount.setForeground(Theme.NAVY);
        text.add(amount, BorderLayout.CENTER);
        if (note != null) text.add(note, BorderLayout.SOUTH);
        body.add(text, BorderLayout.CENTER);
        add(body);
        gradientTop = tint(tone);
        enableHoverGlow();
    }

    private Color gradientTop = Theme.CARD;

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(getWidth(), 0, gradientTop, 0, getHeight(), Theme.CARD));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }

    /** Animates "12,345.67"-style values from zero when the card first appears. */
    private static void countUp(JLabel label, String value) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^-?[0-9][0-9,]*(\\.[0-9]+)?$").matcher(value.trim());
        if (!m.matches()) return;
        java.math.BigDecimal target = new java.math.BigDecimal(value.trim().replace(",", ""));
        int decimals = Math.max(0, target.scale());
        java.text.DecimalFormat format = new java.text.DecimalFormat(decimals == 0 ? "#,##0" : "#,##0." + "0".repeat(decimals),
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
        label.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && label.isShowing()
                    && label.getClientProperty("counted") == null) {
                label.putClientProperty("counted", true);
                Animator.animate(650, t -> label.setText(format.format(target.doubleValue() * t)),
                        () -> label.setText(value));
            }
        });
    }

    static Color accent(Tone tone) {
        return switch (tone) {
            case BLUE -> Theme.PRIMARY;
            case GREEN -> Theme.SUCCESS;
            case ORANGE -> Theme.WARNING;
            default -> Theme.NAVY_3;
        };
    }

    private static Color tint(Tone tone) {
        return switch (tone) {
            case BLUE -> Theme.BABY_LIGHT;
            case GREEN -> new Color(0xE2, 0xF5, 0xEC);
            case ORANGE -> new Color(0xFD, 0xF0, 0xDF);
            default -> new Color(0xE4, 0xEC, 0xF6);
        };
    }

    private static String defaultIcon(Tone tone) {
        return switch (tone) {
            case BLUE -> "receipt";
            case GREEN -> "wallet";
            case ORANGE -> "coins";
            default -> "chart";
        };
    }

    /** Rounded tinted square with the icon in the accent colour. */
    private static final class Badge extends JComponent {
        private final Icon icon;
        private final Color tint;

        Badge(String iconName, Color accent, Color tint) {
            // Solid accent tile with a white icon: the card itself carries the light tint.
            this.icon = Icons.icon(iconName, 22, Color.WHITE);
            this.tint = accent;
            setPreferredSize(new Dimension(46, 46));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 46) / 2;
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 55));
            g.fill(new RoundRectangle2D.Float(1, y + 3, 45, 45, 14, 14));
            g.setPaint(new GradientPaint(0, y, Animator.blend(tint, Color.WHITE, 0.25), 46, y + 46,
                    Animator.blend(tint, Color.BLACK, 0.25)));
            g.fill(new RoundRectangle2D.Float(0, y, 46, 45, 14, 14));
            icon.paintIcon(this, g, 12, y + 12);
            g.dispose();
        }
    }
}
