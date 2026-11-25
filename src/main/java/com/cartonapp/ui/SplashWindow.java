package com.cartonapp.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** Short animated welcome after login: the logo grows in, the name fades in and a bar fills. */
public final class SplashWindow extends JWindow {
    private float progress;

    private SplashWindow() {
        setSize(560, 330);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        setIconImages(BrandLogo.windowIcons());
        JComponent canvas = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintSplash((Graphics2D) graphics.create(), getWidth(), getHeight());
            }
        };
        setContentPane(canvas);
    }

    /** Shows the splash for about 1.4 seconds, then runs {@code next} on the Swing thread. */
    public static void show(Runnable next) {
        SplashWindow splash = new SplashWindow();
        splash.setVisible(true);
        Animator.animate(1400, t -> {
            splash.progress = (float) t;
            splash.repaint();
        }, () -> {
            next.run();
            splash.dispose();
        });
    }

    private void paintSplash(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Shape card = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 28, 28);
        g.setPaint(new GradientPaint(0, 0, Theme.NAVY_3, w, h, Theme.NAVY));
        g.fill(card);
        g.clip(card);
        // drifting glow circles
        g.setColor(new Color(0x8C, 0xC8, 0xF5, 34));
        g.fillOval((int) (w - 260 + 60 * progress), -140, 360, 360);
        g.fillOval((int) (-160 - 40 * progress), h - 170, 300, 300);
        float logo = Math.min(1f, progress / 0.45f);
        int size = (int) (64 + 32 * logo);
        Icon mark = BrandLogo.mark(size);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, logo)));
        mark.paintIcon(this, g, (w - size) / 2, 52 + (96 - size) / 2);
        float text = Math.max(0f, Math.min(1f, (progress - 0.3f) / 0.4f));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, text));
        g.setFont(Theme.font(Font.BOLD, 32));
        g.setColor(Color.WHITE);
        center(g, BrandLogo.NAME, w, 192 + (int) ((1 - text) * 10));
        g.setFont(Theme.font(Font.PLAIN, 15));
        g.setColor(Theme.BABY);
        center(g, BrandLogo.TAGLINE + " — نظام الحسابات", w, 222 + (int) ((1 - text) * 10));
        g.setComposite(AlphaComposite.SrcOver);
        int barW = 300, barX = (w - barW) / 2, barY = 264;
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRoundRect(barX, barY, barW, 8, 8, 8);
        g.setPaint(new GradientPaint(barX, 0, Theme.BABY, barX + barW, 0, Theme.PRIMARY));
        g.fillRoundRect(barX, barY, (int) (barW * progress), 8, 8, 8);
        g.setFont(Theme.font(Font.PLAIN, 12));
        g.setColor(Theme.NAV_TEXT);
        center(g, "جارٍ تجهيز البيانات…", w, 296);
        g.dispose();
    }

    private static void center(Graphics2D g, String text, int width, int baseline) {
        java.awt.font.TextLayout layout = new java.awt.font.TextLayout(text, g.getFont(), g.getFontRenderContext());
        layout.draw(g, (width - layout.getAdvance()) / 2f, baseline);
    }
}
