package com.cartonapp.service;

import com.cartonapp.service.InvoiceTemplateRenderer.Cell;
import com.cartonapp.service.InvoiceTemplateRenderer.Page;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/** Draws a rendered invoice page with Java2D, for the on-screen preview and for printing. */
public final class InvoiceJava2DPainter {
    private InvoiceJava2DPainter() { }

    /** Paints the page in page points (1/72 inch) with the origin at the page's top-left corner. */
    public static void paint(Graphics2D graphics, Page page) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Float(0, 0, InvoiceTemplateRenderer.PAGE_WIDTH, InvoiceTemplateRenderer.PAGE_HEIGHT));
            // Fills are drawn without anti-aliasing so adjacent cells (the two company-band rows) meet
            // without a light seam at fractional coordinates.
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            for (Cell cell : page.cells()) {
                if (cell.fill() == null) continue;
                g.setColor(cell.fill());
                g.fill(new Rectangle2D.Float(cell.x(), cell.y(), cell.width(), cell.height()));
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Cell cell : page.cells()) drawText(g, cell);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(InvoiceTemplateRenderer.BORDER_WIDTH));
            for (Cell cell : page.cells()) {
                if (cell.border()) g.draw(new Rectangle2D.Float(cell.x(), cell.y(), cell.width(), cell.height()));
            }
        } finally {
            g.dispose();
        }
    }

    private static void drawText(Graphics2D g, Cell cell) {
        int count = cell.lines().size();
        if (count == 0) return;
        float available = cell.width() - 2 * InvoiceTemplateRenderer.PADDING;
        float size = cell.fontSize();
        if (cell.shrink()) {
            for (String line : cell.lines()) {
                float width = InvoiceTemplateRenderer.measure(line, size, cell.bold());
                if (width > available && width > 0) size = size * available / width;
            }
        }
        float lineHeight = size * InvoiceTemplateRenderer.LINE_FACTOR;
        float top = cell.y() + (cell.height() - count * lineHeight) / 2;
        g.setColor(cell.color());
        for (int i = 0; i < count; i++) {
            String line = cell.lines().get(i);
            if (line == null || line.isEmpty()) continue;
            TextLayout layout = new TextLayout(InvoiceTemplateRenderer.attributed(line, size, cell.bold()).getIterator(),
                    g.getFontRenderContext());
            float advance = layout.getAdvance();
            float x = switch (cell.align()) {
                case RIGHT -> cell.x() + cell.width() - InvoiceTemplateRenderer.PADDING - advance;
                case LEFT -> cell.x() + InvoiceTemplateRenderer.PADDING;
                case CENTER -> cell.x() + (cell.width() - advance) / 2;
            };
            float baseline = top + i * lineHeight + size * InvoiceTemplateRenderer.BASELINE_FACTOR;
            layout.draw(g, x, baseline);
        }
    }
}
