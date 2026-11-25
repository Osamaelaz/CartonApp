package com.cartonapp.ui;

import java.awt.Color;
import java.util.function.DoubleConsumer;
import javax.swing.Timer;

/** Small Swing-timer animations (ease-out) used for fades, slides, count-ups and hover colours. */
public final class Animator {
    private static final int FRAME_MS = 15;

    private Animator() { }

    /** Calls {@code step} with progress 0..1 (eased) on the Swing thread, then {@code done}. */
    public static Timer animate(int durationMs, DoubleConsumer step, Runnable done) {
        long start = System.currentTimeMillis();
        Timer timer = new Timer(FRAME_MS, null);
        timer.addActionListener(event -> {
            double t = Math.min(1, (System.currentTimeMillis() - start) / (double) durationMs);
            step.accept(1 - Math.pow(1 - t, 3));
            if (t >= 1) {
                timer.stop();
                if (done != null) done.run();
            }
        });
        step.accept(0);
        timer.start();
        return timer;
    }

    public static Color blend(Color from, Color to, double t) {
        return new Color((int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }
}
