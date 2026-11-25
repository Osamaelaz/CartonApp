package com.cartonapp.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** Reusable status badge used by tables and detail headers. */
public final class StatusBadge extends JLabel {
    public enum Tone { GRAY, GREEN, BLUE, ORANGE, RED }

    public StatusBadge(String text, Tone tone) {
        super(text, SwingConstants.CENTER);
        setFont(Theme.font(Font.BOLD, 11));
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        switch (tone) {
            case GREEN -> colors(new Color(0xE2, 0xF4, 0xE9), new Color(0x14, 0x76, 0x3D));
            case BLUE -> colors(new Color(0xE1, 0xEE, 0xFB), new Color(0x15, 0x5F, 0xA7));
            case ORANGE -> colors(new Color(0xFF, 0xF0, 0xD7), new Color(0x9A, 0x5A, 0x05));
            case RED -> colors(new Color(0xFD, 0xE7, 0xE8), new Color(0xB5, 0x2D, 0x36));
            default -> colors(new Color(0xEA, 0xED, 0xF1), Theme.MUTED_TEXT);
        }
    }

    private void colors(Color background, Color foreground) {
        setBackground(background);
        setForeground(foreground);
    }
}
