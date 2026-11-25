package com.cartonapp.ui;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.service.InvoiceExportService;
import com.cartonapp.service.InvoiceJava2DPainter;
import com.cartonapp.service.InvoicePdfService;
import com.cartonapp.service.InvoicePrintService;
import com.cartonapp.service.InvoiceTemplateRenderer;
import com.cartonapp.service.InvoiceTemplateRenderer.Page;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.print.PrinterJob;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * View / print preview of one invoice. The pages shown are the final document produced by the
 * template renderer, the same pages that are printed and written to PDF.
 */
public final class InvoicePreviewDialog extends JDialog {
    private final InvoiceDocument invoice;
    private final CompanyProfile company;
    private final InvoicePrintService printService;
    private final JLabel status = new JLabel(" ", SwingConstants.RIGHT);
    private final JButton openFile;
    private final List<PagePanel> pagePanels = new ArrayList<>();
    private final List<JButton> actions = new ArrayList<>();
    private Path lastFile;

    public InvoicePreviewDialog(JFrame owner, InvoiceDocument invoice, CompanyProfile company,
            InvoicePrintService printService, InvoicePdfService pdfService, InvoiceExportService excelService,
            Consumer<InvoiceDocument> onEdit, Consumer<InvoiceDocument> onDelete) {
        super(owner, com.cartonapp.service.ExportFiles.invoiceName(
                invoice.customer() == null ? null : invoice.customer().name(), invoice.date()) + " — " + invoice.number(), true);
        this.invoice = invoice;
        this.company = company;
        this.printService = printService;
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        toolbar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        toolbar.setBackground(Theme.CARD);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        // The invoice number is in the window title; the toolbar keeps every action visible.
        actions.add(button("طباعة", AppButton.Kind.PRIMARY, "print", this::print));
        actions.add(button("تصدير PDF", AppButton.Kind.SECONDARY, "download",
                () -> export("PDF", () -> pdfService.exportPdf(invoice, company))));
        actions.add(button("تصدير Excel", AppButton.Kind.SECONDARY, "download",
                () -> export("Excel", () -> excelService.exportExcel(invoice, company))));
        actions.add(button("تعديل", AppButton.Kind.SECONDARY, "edit", () -> {
            dispose();
            onEdit.accept(invoice);
        }));
        actions.add(button("حذف", AppButton.Kind.DANGER, "delete", () -> {
            dispose();
            onDelete.accept(invoice);
        }));
        actions.forEach(toolbar::add);
        toolbar.add(button("إغلاق", AppButton.Kind.GHOST, "close", this::dispose));
        add(toolbar, BorderLayout.NORTH);

        JPanel pages = new JPanel();
        pages.setLayout(new BoxLayout(pages, BoxLayout.Y_AXIS));
        pages.setBackground(new Color(0xE4, 0xE8, 0xED));
        pages.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        List<Page> rendered = printService.pages(invoice, company);
        for (Page page : rendered) {
            PagePanel panel = new PagePanel(page);
            pagePanels.add(panel);
            panel.setAlignmentX(Component.CENTER_ALIGNMENT);
            pages.add(panel);
            pages.add(Box.createVerticalStrut(18));
        }
        JScrollPane scroll = new JScrollPane(pages);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                float zoom = Math.max(0.5f, Math.min(2f,
                        (scroll.getViewport().getWidth() - 60) / InvoiceTemplateRenderer.PAGE_WIDTH));
                pagePanels.forEach(panel -> panel.setZoom(zoom));
                pages.revalidate();
            }
        });
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        footer.setBackground(Theme.SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        status.setFont(Theme.font(Font.PLAIN, 12));
        status.setForeground(Theme.MUTED_TEXT);
        status.setText(rendered.size() == 1 ? "صفحة واحدة — A4" : rendered.size() + " صفحات — A4");
        footer.add(status, BorderLayout.CENTER);
        openFile = button("فتح الملف", AppButton.Kind.GHOST, null, this::openLastFile);
        openFile.setVisible(false);
        footer.add(openFile, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(980, screen.width - 60), Math.min(980, screen.height - 60));
        setLocationRelativeTo(owner);
    }

    private void print() {
        PrinterJob job = printService.createJob(invoice, company);
        if (!job.printDialog()) return;
        setBusy(true, "جارٍ إرسال الفاتورة إلى الطابعة…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                job.print();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setBusy(false, "تم إرسال الفاتورة إلى الطابعة.");
                } catch (Exception exception) {
                    setBusy(false, "تعذرت الطباعة: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void export(String kind, Callable<Path> task) {
        setBusy(true, "جارٍ تصدير الفاتورة إلى " + kind + "…");
        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    lastFile = get().toAbsolutePath();
                    setBusy(false, "تم حفظ ملف " + kind + ": " + lastFile);
                    openFile.setVisible(Desktop.isDesktopSupported());
                } catch (Exception exception) {
                    setBusy(false, "تعذر تصدير " + kind + ": " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void openLastFile() {
        if (lastFile == null) return;
        try {
            Desktop.getDesktop().open(lastFile.toFile());
        } catch (Exception exception) {
            status.setText("تعذر فتح الملف: " + exception.getMessage());
        }
    }

    private void setBusy(boolean busy, String message) {
        actions.forEach(button -> button.setEnabled(!busy));
        status.setForeground(message.startsWith("تعذر") ? Theme.ERROR : Theme.MUTED_TEXT);
        status.setText(message);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static JButton button(String text, AppButton.Kind kind, String icon, Runnable action) {
        JButton button = icon == null ? new AppButton(text, kind) : new AppButton(text, kind, icon);
        button.addActionListener(event -> action.run());
        return InvoiceEditorDialog.fitText(button);
    }

    /** One white A4 sheet drawn with the shared Java2D painter. */
    private static final class PagePanel extends JComponent {
        private final Page page;
        private float zoom = 1.3f;

        PagePanel(Page page) {
            this.page = page;
            updateSize();
        }

        void setZoom(float zoom) {
            if (Math.abs(this.zoom - zoom) < 0.001f) return;
            this.zoom = zoom;
            updateSize();
            repaint();
        }

        private void updateSize() {
            Dimension size = new Dimension(Math.round(InvoiceTemplateRenderer.PAGE_WIDTH * zoom),
                    Math.round(InvoiceTemplateRenderer.PAGE_HEIGHT * zoom));
            setPreferredSize(size);
            setMaximumSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(new Color(0, 0, 0, 30));
                g.fillRect(3, 3, getWidth() - 3, getHeight() - 3);
                g.scale(zoom, zoom);
                InvoiceJava2DPainter.paint(g, page);
            } finally {
                g.dispose();
            }
        }
    }
}
