package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.service.InvoiceTemplateRenderer.Page;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.List;

/**
 * Prints one invoice as a real document (not a screenshot) from the shared template layout, on A4
 * portrait. The caller shows {@link PrinterJob#printDialog()} on the Swing thread and calls
 * {@link PrinterJob#print()} in the background.
 */
public final class InvoicePrintService {
    private final InvoiceTemplateRenderer renderer = new InvoiceTemplateRenderer();

    public List<Page> pages(InvoiceDocument invoice, CompanyProfile company) {
        return renderer.render(invoice, company);
    }

    public PrinterJob createJob(InvoiceDocument invoice, CompanyProfile company) {
        List<Page> pages = pages(invoice, company);
        PrinterJob job = PrinterJob.getPrinterJob();
        // When printing to PDF, Windows suggests the job name as the file name.
        job.setJobName(ExportFiles.invoiceName(invoice.customer() == null ? null : invoice.customer().name(),
                invoice.date()));
        PageFormat format = job.defaultPage();
        Paper paper = new Paper();
        paper.setSize(InvoiceTemplateRenderer.PAGE_WIDTH, InvoiceTemplateRenderer.PAGE_HEIGHT);
        paper.setImageableArea(0, 0, InvoiceTemplateRenderer.PAGE_WIDTH, InvoiceTemplateRenderer.PAGE_HEIGHT);
        format.setPaper(paper);
        format.setOrientation(PageFormat.PORTRAIT);
        format = job.validatePage(format);
        job.setPrintable(new InvoicePrintable(pages), format);
        return job;
    }

    private record InvoicePrintable(List<Page> pages) implements Printable {
        @Override
        public int print(Graphics graphics, PageFormat format, int pageIndex) {
            if (pageIndex >= pages.size()) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                fitContent(g, format);
                InvoiceJava2DPainter.paint(g, pages.get(pageIndex));
                return PAGE_EXISTS;
            } finally {
                g.dispose();
            }
        }

        /**
         * Keeps the template at its true A4 position when the printer's printable area contains the
         * content; otherwise scales the content box down into the printable area.
         */
        private static void fitContent(Graphics2D g, PageFormat format) {
            float margin = InvoiceTemplateRenderer.MARGIN;
            double contentWidth = InvoiceTemplateRenderer.PAGE_WIDTH - 2 * margin;
            double contentHeight = InvoiceTemplateRenderer.PAGE_HEIGHT - 2 * margin;
            double ix = format.getImageableX();
            double iy = format.getImageableY();
            double iw = format.getImageableWidth();
            double ih = format.getImageableHeight();
            boolean fits = ix <= margin && iy <= margin && ix + iw >= margin + contentWidth
                    && iy + ih >= margin + contentHeight;
            if (fits) return;
            double scale = Math.min(1, Math.min(iw / contentWidth, ih / contentHeight));
            g.translate(ix + (iw - contentWidth * scale) / 2, iy);
            g.scale(scale, scale);
            g.translate(-margin, -margin);
        }
    }
}
