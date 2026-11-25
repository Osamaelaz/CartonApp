package com.cartonapp.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** Shimmering placeholder cards and rows shown while a page loads (instead of plain "loading" text). */
public final class SkeletonPanel extends JComponent {
    private final Timer timer;
    private float phase;

    public SkeletonPanel() {
        setPreferredSize(new Dimension(1, 420));
        timer = new Timer(30, e -> {
            phase = (phase + 0.018f) % 1.6f;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int gap = 12;
        int cardW = (w - 2 * gap) / 3;
        for (int i = 0; i < 3; i++) block(g, i * (cardW + gap), 0, cardW, 78);
        block(g, 0, 96, w, 300);
        g.setColor(new Color(0xD6, 0xE4, 0xF4));
        for (int r = 0; r < 6; r++) {
            int y = 150 + r * 40;
            bar(g, w - 40 - (int) (w * 0.22), y, (int) (w * 0.22), 12);
            bar(g, (int) (w * 0.42), y, (int) (w * 0.18), 12);
            bar(g, 40, y, (int) (w * 0.14), 12);
        }
        bar(g, w - 40 - (int) (w * 0.3), 118, (int) (w * 0.3), 16);
        // moving highlight across everything
        int x = (int) ((phase - 0.3f) * w);
        // one soft band of light: transparent -> white -> transparent
        g.setPaint(new LinearGradientPaint(x, 0, x + 260, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, 150), new Color(255, 255, 255, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.setComposite(AlphaComposite.SrcAtop);
        g.fillRect(0, 0, w, getHeight());
        g.dispose();
    }

    private static void block(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0xE6, 0xEF, 0xFA));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 14, 14));
        g.setColor(new Color(0xCF, 0xDE, 0xF0));
        g.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, 14, 14));
        if (h < 100) {
            g.setColor(new Color(0xD3, 0xE2, 0xF4));
            g.fill(new RoundRectangle2D.Float(x + w - 62, y + 16, 46, 46, 14, 14));
            bar(g, x + w - 62 - 20 - (int) (w * 0.35), y + 22, (int) (w * 0.35), 10);
            bar(g, x + w - 62 - 20 - (int) (w * 0.5), y + 44, (int) (w * 0.5), 16);
        }
    }

    private static void bar(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0xD6, 0xE4, 0xF4));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
    }
}
