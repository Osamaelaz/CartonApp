package com.cartonapp.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.ComponentOrientation;
import javax.swing.JButton;

/** Shared compact action button for desktop screens. */
public final class AppButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }

    public AppButton(String text, Kind kind) {
        super(text);
        setFont(Theme.font(Font.BOLD, 12));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setHorizontalTextPosition(TRAILING);
        setVerticalTextPosition(CENTER);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(110, 34));
        setOpaque(true);
        switch (kind) {
            case PRIMARY -> { setBackground(Theme.PRIMARY); setForeground(java.awt.Color.WHITE); gradient = true;
                setContentAreaFilled(false); setBorderPainted(false); }
            case DANGER -> { setBackground(Theme.CARD); setForeground(new java.awt.Color(0xB4, 0x23, 0x2E)); }
            case GHOST -> { setBackground(Theme.CARD); setForeground(Theme.PRIMARY); setBorderPainted(false); }
            default -> { setBackground(Theme.CARD); setForeground(Theme.NAVY); }
        }
    }

    private boolean gradient;

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        if (gradient) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            boolean down = getModel().isPressed();
            java.awt.Color top = down ? Theme.NAVY_3 : hover ? new java.awt.Color(0x3A, 0x86, 0xEA) : Theme.PRIMARY;
            java.awt.Color bottom = down ? Theme.NAVY : Theme.NAVY_3;
            if (!isEnabled()) { top = new java.awt.Color(0x9F, 0xBC, 0xDD); bottom = top; }
            g.setColor(new java.awt.Color(0x1E, 0x6F, 0xD9, hover ? 70 : 40));
            g.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 10, 10);
            g.setPaint(new java.awt.GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 10, 10);
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    public AppButton(String text, Kind kind, String iconName) {
        this(text, kind);
        setIcon(Icons.icon(iconName, 16, getForeground()));
        setIconTextGap(7);
    }
}
