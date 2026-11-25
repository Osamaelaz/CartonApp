package com.cartonapp.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;

/** Shared primary action button. */
public final class PrimaryButton extends JButton {
    public PrimaryButton(String text) {
        super(text);
        setFont(Theme.font(Font.BOLD, 13));
        setForeground(Theme.CARD);
        setBackground(Theme.PRIMARY);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(1, Theme.BUTTON_HEIGHT));
    }
}
