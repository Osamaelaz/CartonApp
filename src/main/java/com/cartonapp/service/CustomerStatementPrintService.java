package com.cartonapp.service;

import com.cartonapp.model.StatementEntry;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.util.List;

/** Sends a database-backed customer statement to the selected Windows printer. */
public final class CustomerStatementPrintService {
    private static final String COMPANY_NAME = "الليثي باك";

    public boolean print(String customerName, List<StatementEntry> entries) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(ExportFiles.statementName(customerName, java.time.LocalDate.now()));
        job.setPrintable(new StatementPrintable(customerName, entries));
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    private static final class StatementPrintable implements Printable {
        private final String customerName;
        private final List<StatementEntry> entries;

        StatementPrintable(String customerName, List<StatementEntry> entries) {
            this.customerName = customerName == null ? "" : customerName;
            this.entries = entries;
        }

        @Override
        public int print(Graphics graphics, PageFormat format, int pageIndex) {
            int rowsPerPage = 24;
            int pageCount = Math.max(1, (entries.size() + rowsPerPage - 1) / rowsPerPage);
            if (pageIndex >= pageCount) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, (int) format.getImageableWidth(), (int) format.getImageableHeight());
                g.setColor(new Color(0x1D, 0x4E, 0x89));
                g.setFont(new Font("Tahoma", Font.BOLD, 18));
                drawRight(g, COMPANY_NAME, format.getImageableWidth() - 20, 32);
                g.setFont(new Font("Tahoma", Font.PLAIN, 12));
                g.setColor(new Color(0x25, 0x31, 0x3D));
                drawRight(g, "كشف حساب: " + customerName, format.getImageableWidth() - 20, 54);

                int y = 88;
                g.setColor(new Color(0xE7, 0xF1, 0xFB));
                g.fillRect(20, y - 16, (int) format.getImageableWidth() - 40, 24);
                g.setColor(new Color(0x25, 0x31, 0x3D));
                g.setFont(new Font("Tahoma", Font.BOLD, 10));
                int right = (int) format.getImageableWidth() - 25;
                drawRight(g, "التاريخ", right, y);
                drawRight(g, "البيان", right - 90, y);
                drawRight(g, "مدين", right - 280, y);
                drawRight(g, "دائن", right - 360, y);
                drawRight(g, "الرصيد", right - 440, y);
                g.setFont(new Font("Tahoma", Font.PLAIN, 9));
                int start = pageIndex * rowsPerPage;
                int end = Math.min(start + rowsPerPage, entries.size());
                for (int index = start; index < end; index++) {
                    StatementEntry entry = entries.get(index);
                    y += 24;
                    if ((index - start) % 2 == 0) {
                        g.setColor(new Color(0xF8, 0xFA, 0xFC));
                        g.fillRect(20, y - 16, (int) format.getImageableWidth() - 40, 22);
                    }
                    g.setColor(new Color(0x25, 0x31, 0x3D));
                    drawRight(g, arabicDigits(entry.date().toString()), right, y);
                    drawRight(g, entry.description(), right - 90, y);
                    drawRight(g, money(entry.debit()), right - 280, y);
                    drawRight(g, money(entry.credit()), right - 360, y);
                    drawRight(g, money(entry.balance()), right - 440, y);
                }
                g.setColor(new Color(0x1D, 0x4E, 0x89));
                g.setFont(new Font("Tahoma", Font.PLAIN, 9));
                drawRight(g, "الصفحة " + arabicDigits(Integer.toString(pageIndex + 1)), right, y + 34);
                return PAGE_EXISTS;
            } finally {
                g.dispose();
            }
        }

        private static void drawRight(Graphics2D g, String value, double right, int y) {
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(value == null ? "" : value, (int) right - metrics.stringWidth(value == null ? "" : value), y);
        }
    }

    private static String money(BigDecimal value) {
        return arabicDigits((value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString());
    }

    private static String arabicDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            if (character >= '0' && character <= '9') result.append((char) ('\u0660' + character - '0'));
            else if (character == '.') result.append('\u066B');
            else result.append(character);
        }
        return result.toString();
    }
}
