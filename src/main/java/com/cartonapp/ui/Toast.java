package com.cartonapp.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.Timer;

/** Lightweight reusable confirmation toast. */
public final class Toast extends JLabel {
    public Toast() {
        setOpaque(true);
        setBackground(Theme.NAVY);
        setForeground(Color.WHITE);
        setFont(Theme.font(Font.PLAIN, 12));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.BABY),
                BorderFactory.createEmptyBorder(11, 16, 11, 16)));
        setVisible(false);
    }

    private double progress = 1;
    private Timer hide;
    private long shownAt;
    private final Timer countdown = new Timer(40, e -> repaint());

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        if (!isVisible() || shownAt == 0) return;
        double left = Math.max(0, 1 - (System.currentTimeMillis() - shownAt) / 3000.0);
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        g.setPaint(new java.awt.GradientPaint(0, 0, Theme.BABY, getWidth(), 0, Theme.PRIMARY));
        g.fillRect(getWidth() - (int) (getWidth() * left), getHeight() - 3, (int) (getWidth() * left), 3);
        g.dispose();
        if (left <= 0) countdown.stop();
    }

    @Override
    public java.awt.Dimension getPreferredSize() {
        java.awt.Dimension size = super.getPreferredSize();
        return new java.awt.Dimension(size.width, (int) Math.round(size.height * progress));
    }

    public void showMessage(String message) {
        setText("✓  " + message);
        setVisible(true);
        shownAt = System.currentTimeMillis();
        countdown.start();
        if (hide != null) hide.stop();
        Animator.animate(220, t -> {
            progress = t;
            setBackground(Animator.blend(Theme.BABY, Theme.NAVY, t));
            revalidate();
        }, null);
        hide = new Timer(3000, event -> Animator.animate(200, t -> {
            progress = 1 - t;
            revalidate();
        }, () -> setVisible(false)));
        hide.setRepeats(false);
        hide.start();
    }
}
