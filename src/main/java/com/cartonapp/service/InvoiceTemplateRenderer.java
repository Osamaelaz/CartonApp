package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.Customer;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceDocumentLine;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceTotals;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.math.BigDecimal;
import java.text.AttributedString;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lays out one invoice as A4 pages following the official Excel template
 * ({@code فاتورة_مبيعات_تجريبيه.xlsx}, sheet "فاتورة مبيعات"): the same rows, merged cells,
 * column proportions, fills, fonts, borders and number formats, scaled uniformly so the seven
 * template columns fill the A4 width. The result is a list of positioned cells that the Java2D
 * painter (preview and print) and the PDF writer draw identically.
 *
 * <p>Template map (Excel rows): 1-2 company band, 4 title/number/date, 6-8 customer, 10 table
 * header, 11-20 lines (at least ten rows), 22-25 totals, 25-26 receipt acknowledgement, 28
 * signatures. Invoices with more lines continue on further pages that repeat the company band,
 * the invoice title row and the table header.
 */
public final class InvoiceTemplateRenderer {
    public static final float PAGE_WIDTH = 595.2756f;
    public static final float PAGE_HEIGHT = 841.8898f;
    public static final float MARGIN = 24f;
    /** Line height and baseline position as multiples of the font size; shared by all painters. */
    public static final float LINE_FACTOR = 1.2f;
    public static final float BASELINE_FACTOR = 0.95f;
    public static final float PADDING = 3f;
    public static final float BORDER_WIDTH = 0.6f;

    /** Template column widths in pixels (Excel widths 14, 32, 14, 14, 16, 18, 18 for columns A-G). */
    private static final float[] COLUMN_PIXELS = {101, 227, 101, 101, 115, 129, 129};
    private static final float SCALE;
    private static final float[] COLUMN_WIDTHS = new float[7];

    static final Color NAVY = new Color(0x20, 0x37, 0x64);
    static final Color ORANGE = new Color(0xED, 0x7D, 0x31);
    static final Color GRAY = new Color(0xD9, 0xD9, 0xD9);
    /** Theme accent 1 (#4F81BD) with 60% tint, the fill of template rows 1-2. */
    static final Color BAND = new Color(0xB8, 0xCC, 0xE4);
    static final Color RED = new Color(0xC0, 0x00, 0x00);
    static final Color BLACK = Color.BLACK;

    static final String ACKNOWLEDGEMENT = "أستلمت أنا / المقر أدناه البضاعة الموضحة أعلاه من الشركة \"في حالة سليمة"
            + " وخالية من أي عيوب ومسؤولاً عن تخزينها وعرضها طبقاً لأصول التخزين وأتعهد أنا بسداد باقي قيمة"
            + " الفاتورة (المتبقي) في الفاتورة\"";

    private static final int TEMPLATE_LINE_ROWS = 10;
    private static final float ROW = 14.4f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    static {
        float totalPoints = 0;
        for (float pixels : COLUMN_PIXELS) totalPoints += pixels * 0.75f;
        SCALE = (PAGE_WIDTH - 2 * MARGIN) / totalPoints;
        for (int i = 0; i < 7; i++) COLUMN_WIDTHS[i] = COLUMN_PIXELS[i] * 0.75f * SCALE;
    }

    public enum Align { RIGHT, CENTER, LEFT }

    /**
     * One rectangle on a page, in points from the top-left corner. {@code lines} are already wrapped;
     * {@code shrink} asks painters to reduce the font size if a line is wider than the cell.
     */
    public record Cell(float x, float y, float width, float height, Color fill, boolean border, List<String> lines,
            float fontSize, boolean bold, Color color, Align align, boolean shrink) {
    }

    public record Page(int number, int count, List<Cell> cells) {
    }

    private enum Kind { LINE, PADDING }

    private record TableRow(Kind kind, int serial, InvoiceDocumentLine line, List<String> nameLines, float height) {
    }

    public List<Page> render(InvoiceDocument invoice, CompanyProfile company) {
        InvoiceTotals totals = InvoiceCalculator.totals(invoice.invoiceLines(), invoice.previousBalance(),
                invoice.additionalDiscount(), invoice.cashPaid());
        List<TableRow> rows = tableRows(invoice);
        float bottom = PAGE_HEIGHT - MARGIN - pt(ROW);
        List<List<Cell>> pages = new ArrayList<>();

        List<Cell> page = new ArrayList<>();
        float y = firstPageHeader(page, invoice, company);
        for (TableRow row : rows) {
            if (y + row.height() > bottom) {
                pages.add(page);
                page = new ArrayList<>();
                y = continuationHeader(page, invoice, company, true);
            }
            y = tableRow(page, row, y);
        }
        float summaryHeight = summaryHeight();
        if (y + summaryHeight > bottom) {
            pages.add(page);
            page = new ArrayList<>();
            y = continuationHeader(page, invoice, company, false);
        }
        summary(page, totals, y);
        pages.add(page);

        List<Page> result = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            List<Cell> cells = pages.get(i);
            if (pages.size() > 1) {
                cells.add(text(MARGIN, PAGE_HEIGHT - MARGIN - pt(ROW), PAGE_WIDTH - 2 * MARGIN, pt(ROW),
                        "صفحة " + (i + 1) + " من " + pages.size(), 9, false, BLACK, Align.CENTER));
            }
            result.add(new Page(i + 1, pages.size(), List.copyOf(cells)));
        }
        return result;
    }

    // ----- template sections -------------------------------------------------------------------

    /** Excel rows 1-10. Returns the y position below the table header. */
    private float firstPageHeader(List<Cell> cells, InvoiceDocument invoice, CompanyProfile company) {
        float y = companyBand(cells, company, MARGIN);
        y += pt(ROW); // row 3
        y = titleRow(cells, invoice, y);
        y += pt(ROW); // row 5
        Customer customer = invoice.customer();
        String name = customer == null ? "" : customer.name();
        String address = customer == null ? "" : customer.address();
        y = customerRow(cells, "العميل", name, y);
        y = customerRow(cells, "العنوان", address, y);
        y = customerRow(cells, "اسم الشركة", name, y);
        y += pt(ROW); // row 9
        return tableHeader(cells, y);
    }

    private float continuationHeader(List<Cell> cells, InvoiceDocument invoice, CompanyProfile company,
            boolean withTableHeader) {
        float y = companyBand(cells, company, MARGIN);
        y += pt(ROW);
        y = titleRow(cells, invoice, y);
        y += pt(ROW);
        return withTableHeader ? tableHeader(cells, y) : y;
    }

    /** Rows 1-2: A1:G1 company name (bold 18, #203764) and A2:G2 address | phone (bold 12) on the band fill. */
    private float companyBand(List<Cell> cells, CompanyProfile company, float y) {
        float x = colX(6);
        float width = PAGE_WIDTH - 2 * MARGIN;
        cells.add(box(x, y, width, pt(28.2f), BAND, false, List.of(company.name()), 18, true, NAVY, Align.CENTER, true));
        y += pt(28.2f);
        cells.add(box(x, y, width, pt(ROW), BAND, false, List.of(company.contactLine()), 12, true, BLACK, Align.CENTER,
                true));
        return y + pt(ROW);
    }

    /** Row 4: C4:D4 title, E4 invoice number, F4 "تاريخ البيان", G4 date. */
    private float titleRow(List<Cell> cells, InvoiceDocument invoice, float y) {
        float h = pt(21f);
        cells.add(spanBox(2, 3, y, h, ORANGE, true, "فاتورة مبيعات", 12, true, Color.WHITE, Align.CENTER));
        cells.add(spanBox(4, 4, y, h, null, true, invoice.number(), 16, true, RED, Align.CENTER));
        cells.add(spanBox(5, 5, y, h, GRAY, true, "تاريخ البيان", 11, true, BLACK, Align.CENTER));
        cells.add(spanBox(6, 6, y, h, null, true, invoice.date() == null ? "" : DATE.format(invoice.date()), 11, true,
                BLACK, Align.CENTER));
        return y + h;
    }

    /** Rows 6-8: label in A (gray), value merged B:D, right aligned, wrapping long values. */
    private float customerRow(List<Cell> cells, String label, String value, float y) {
        float valueWidth = spanWidth(1, 3);
        List<String> lines = wrap(value, valueWidth - 2 * PADDING, pt(11), false);
        float h = Math.max(pt(ROW), lines.size() * pt(11) * LINE_FACTOR + 2);
        cells.add(spanBox(0, 0, y, h, GRAY, true, label, 11, true, BLACK, Align.CENTER));
        cells.add(new Cell(colX(3), y, valueWidth, h, null, true, lines, pt(11), false, BLACK, Align.RIGHT, true));
        return y + h;
    }

    /** Row 10: white bold 12 on #203764. */
    private float tableHeader(List<Cell> cells, float y) {
        String[] headings = {"مسلسل", "الصنف", "الكمية", "الوحدة", "القيمة", "قيمة الخصم", "الصافي"};
        float h = pt(15.6f);
        for (int c = 0; c < headings.length; c++)
            cells.add(spanBox(c, c, y, h, NAVY, true, headings[c], 12, true, Color.WHITE, Align.CENTER));
        return y + h;
    }

    private List<TableRow> tableRows(InvoiceDocument invoice) {
        List<TableRow> rows = new ArrayList<>();
        float nameWidth = COLUMN_WIDTHS[1] - 2 * PADDING;
        int serial = 1;
        for (InvoiceDocumentLine line : invoice.lines()) {
            List<String> name = wrap(line.itemName(), nameWidth, pt(11), false);
            float h = Math.max(pt(ROW), name.size() * pt(11) * LINE_FACTOR + 2);
            rows.add(new TableRow(Kind.LINE, serial++, line, name, h));
        }
        while (rows.size() < TEMPLATE_LINE_ROWS)
            rows.add(new TableRow(Kind.PADDING, serial++, null, List.of(), pt(ROW)));
        return rows;
    }

    /** Rows 11-20 cell styles: serial bold centred, item right aligned, values in #,##0.00. */
    private float tableRow(List<Cell> cells, TableRow row, float y) {
        float h = row.height();
        cells.add(spanBox(0, 0, y, h, null, true, String.valueOf(row.serial()), 11, true, BLACK, Align.CENTER));
        if (row.kind() == Kind.PADDING) {
            for (int c = 1; c < 7; c++) cells.add(spanBox(c, c, y, h, null, true, "", 11, false, BLACK, Align.CENTER));
            return y + h;
        }
        InvoiceLine line = row.line().line();
        cells.add(new Cell(colX(1), y, COLUMN_WIDTHS[1], h, null, true, row.nameLines(), pt(11), false, BLACK,
                Align.RIGHT, true));
        cells.add(spanBox(2, 2, y, h, null, true, decimal(line.quantity()), 11, false, BLACK, Align.CENTER));
        cells.add(spanBox(3, 3, y, h, null, true, decimal(line.unitPrice()), 11, false, BLACK, Align.CENTER));
        cells.add(spanBox(4, 4, y, h, null, true, decimal(InvoiceCalculator.lineValue(line)), 11, true, BLACK,
                Align.CENTER));
        String discount = line.discount().signum() == 0 ? "" : decimal(line.discount());
        cells.add(spanBox(5, 5, y, h, null, true, discount, 11, false, BLACK, Align.CENTER));
        cells.add(spanBox(6, 6, y, h, null, true, decimal(InvoiceCalculator.lineNet(line)), 11, true, BLACK,
                Align.CENTER));
        return y + h;
    }

    private float acknowledgementHeight() {
        List<String> lines = wrap(ACKNOWLEDGEMENT, spanWidth(0, 4) - 2 * PADDING, pt(10), true);
        return Math.max(pt(28.8f) + pt(ROW), lines.size() * pt(10) * LINE_FACTOR + 4);
    }

    private float summaryHeight() {
        float rows22to24 = 3 * pt(ROW);
        float rows25to26 = acknowledgementHeight();
        return pt(ROW) + rows22to24 + rows25to26 + pt(ROW) + pt(ROW);
    }

    /** Rows 21-28. */
    private void summary(List<Cell> cells, InvoiceTotals totals, float y) {
        y += pt(ROW); // row 21
        float r = pt(ROW);
        // row 22
        cells.add(label(0, y, r, "المدفوع نقداً"));
        cells.add(value(1, y, r, decimal(totals.cashPaid())));
        cells.add(value(2, y, r, quantity(totals.totalQuantity())));
        cells.add(label(3, y, r, "إجمالي الكميات"));
        cells.add(label(5, y, r, "قيمة الفاتورة"));
        cells.add(value(6, y, r, decimal(totals.invoiceValue())));
        y += r;
        // row 23
        cells.add(label(0, y, r, "الرصيد النهائي"));
        cells.add(value(1, y, r, decimal(totals.finalBalance())));
        cells.add(label(5, y, r, "الرصيد السابق"));
        cells.add(value(6, y, r, decimal(totals.previousBalance())));
        y += r;
        // row 24
        cells.add(label(5, y, r, "خصم إضافي"));
        cells.add(value(6, y, r, decimal(totals.additionalDiscount())));
        y += r;
        // rows 25-26: acknowledgement merged A25:E26 (bold 10, #C00000); F25/G25 total with debt
        float ackHeight = acknowledgementHeight();
        List<String> ack = wrap(ACKNOWLEDGEMENT, spanWidth(0, 4) - 2 * PADDING, pt(10), true);
        cells.add(new Cell(colX(4), y, spanWidth(0, 4), ackHeight, null, false, ack, pt(10), true, RED, Align.CENTER,
                true));
        float row25 = pt(28.8f);
        cells.add(label(5, y, row25, "إجمالي الفاتورة والمديونية"));
        cells.add(value(6, y, row25, decimal(totals.totalWithDebt())));
        y += ackHeight;
        y += pt(ROW); // row 27
        // row 28: signatures, bold 11, no borders
        cells.add(spanBox(0, 0, y, r, null, false, "المستلم", 11, true, BLACK, Align.CENTER));
        cells.add(spanBox(2, 2, y, r, null, false, "مسؤول المبيعات", 11, true, BLACK, Align.CENTER));
        cells.add(spanBox(5, 5, y, r, null, false, "الختم", 11, true, BLACK, Align.CENTER));
    }

    private Cell label(int column, float y, float h, String text) {
        return spanBox(column, column, y, h, GRAY, true, text, 11, true, BLACK, Align.CENTER);
    }

    private Cell value(int column, float y, float h, String text) {
        return spanBox(column, column, y, h, null, true, text, 11, true, BLACK, Align.CENTER);
    }

    // ----- geometry ---------------------------------------------------------------------------

    /** Excel points to page points. */
    static float pt(float excelPoints) {
        return excelPoints * SCALE;
    }

    /** Left x of a template column; column A (0) is the rightmost in the right-to-left sheet. */
    static float colX(int column) {
        float right = PAGE_WIDTH - MARGIN;
        for (int c = 0; c < column; c++) right -= COLUMN_WIDTHS[c];
        return right - COLUMN_WIDTHS[column];
    }

    static float spanWidth(int from, int to) {
        float width = 0;
        for (int c = from; c <= to; c++) width += COLUMN_WIDTHS[c];
        return width;
    }

    private Cell spanBox(int from, int to, float y, float h, Color fill, boolean border, String text, float excelSize,
            boolean bold, Color color, Align align) {
        return box(colX(to), y, spanWidth(from, to), h, fill, border, List.of(text == null ? "" : text), excelSize,
                bold, color, align, true);
    }

    private Cell box(float x, float y, float w, float h, Color fill, boolean border, List<String> lines,
            float excelSize, boolean bold, Color color, Align align, boolean shrink) {
        return new Cell(x, y, w, h, fill, border, lines, pt(excelSize), bold, color, align, shrink);
    }

    private Cell text(float x, float y, float w, float h, String text, float size, boolean bold, Color color,
            Align align) {
        return new Cell(x, y, w, h, null, false, List.of(text), size, bold, color, align, true);
    }

    // ----- text ---------------------------------------------------------------------------------

    /** Advance width of logical text in points, measured with the shared invoice font. */
    public static float measure(String text, float size, boolean bold) {
        if (text == null || text.isEmpty()) return 0;
        return new TextLayout(attributed(text, size, bold).getIterator(), FRC).getAdvance();
    }

    /** Right-to-left paragraph with the invoice font; used for measuring and Java2D drawing. */
    public static AttributedString attributed(String text, float size, boolean bold) {
        AttributedString value = new AttributedString(text);
        Font font = InvoiceFonts.awt(bold).deriveFont(size);
        value.addAttribute(TextAttribute.FONT, font);
        value.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        return value;
    }

    /** Word-wraps text to the width; very long words are split so nothing is clipped. */
    static List<String> wrap(String text, float width, float size, boolean bold) {
        String value = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        List<String> lines = new ArrayList<>();
        if (value.isEmpty() || measure(value, size, bold) <= width) {
            lines.add(value);
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measure(candidate, size, bold) <= width) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) lines.add(current.toString());
            current.setLength(0);
            String rest = word;
            while (measure(rest, size, bold) > width && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && measure(rest.substring(0, cut), size, bold) > width) cut--;
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            current.append(rest);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    // ----- number formats -----------------------------------------------------------------------

    /** Template number format #,##0.00. */
    static String decimal(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(InvoiceCalculator.money(value));
    }

    /** Template format #,##0 for the quantity total; fractions are kept when present. */
    static String quantity(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.stripTrailingZeros().scale() <= 0)
            return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
        return decimal(amount);
    }
}
