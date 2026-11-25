package com.cartonapp.ui;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * Shared content card with softly rounded corners and a soft shadow. Children (headers, tables,
 * scroll panes) are clipped to the rounded shape. When a page opens, cards slide up and fade in one
 * after another; cards that opt in also glow when the mouse is over them.
 */
public class AppCard extends JPanel {
    public static final int RADIUS = 14;
    private static final int MAX_STAGGER = 6;

    private float appear = 0f;
    private float glow = 0f;
    private boolean entered;

    public AppCard() {
        super(new BorderLayout());
        setOpaque(false);
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !entered) {
                entered = true;
                startEntrance();
            }
        });
    }

    /** Glow and lift slightly while the mouse is over the card (used by key-figure cards). */
    protected void enableHoverGlow() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                fadeGlow(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!contains(e.getPoint())) fadeGlow(0f);
            }
        });
    }

    private void fadeGlow(float target) {
        float from = glow;
        Animator.animate(180, t -> {
            glow = (float) (from + (target - from) * t);
            repaint();
        }, null);
    }

    private void startEntrance() {
        int index = 0;
        Container parent = getParent();
        if (parent != null) {
            for (Component sibling : parent.getComponents()) {
                if (sibling == this) break;
                if (sibling instanceof AppCard) index++;
            }
        }
        int delay = Math.min(index, MAX_STAGGER) * 70;
        Timer wait = new Timer(delay, e -> Animator.animate(380, t -> {
            appear = (float) t;
            repaint();
        }, null));
        wait.setRepeats(false);
        wait.start();
    }

    private Shape shape(float lift) {
        return new RoundRectangle2D.Float(0, -lift, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
    }

    @Override
    public void paint(Graphics graphics) {
        if (appear <= 0f) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (appear < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, appear));
            g.translate(0, (1 - appear) * 16);
        }
        // shadow: deeper and bluish when glowing
        g.setColor(new Color(0x0A + (int) (0x14 * glow), 0x25 + (int) (0x4A * glow), 0x40 + (int) (0x99 * glow),
                18 + (int) (40 * glow)));
        g.fill(new RoundRectangle2D.Float(0, 2, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS));
        Shape shape = shape(0);
        Graphics2D inner = (Graphics2D) g.create();
        inner.clip(shape);
        inner.setColor(getBackground());
        inner.fill(shape);
        super.paint(inner);
        inner.dispose();
        g.setColor(Animator.blend(Theme.BORDER, Theme.PRIMARY, glow));
        g.setStroke(new BasicStroke(1f + glow));
        g.draw(shape);
        g.dispose();
    }
}
