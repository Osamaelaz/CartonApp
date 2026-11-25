package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.service.ReportDocument.Column;
import com.cartonapp.service.ReportDocument.Field;
import com.cartonapp.service.ReportDocument.Kind;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes a {@link ReportDocument} to Excel and PDF in the {@code exports} folder, using the same
 * visual language as the sales-invoice template: blue company band, orange title, gray labels,
 * navy table header, Arial, right-to-left and two-decimal money.
 */
public final class ReportExportService {
    private static final Color NAVY = new Color(0x20, 0x37, 0x64);
    private static final Color ORANGE = new Color(0xED, 0x7D, 0x31);
    private static final Color GRAY = new Color(0xD9, 0xD9, 0xD9);
    private static final Color BAND = new Color(0xB8, 0xCC, 0xE4);
    private static final Color STRIPE = new Color(0xF3, 0xF6, 0xFA);
    private static final Color TOTAL = new Color(0xE7, 0xEC, 0xF3);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final CompanyProfile company;

    public ReportExportService(CompanyProfile company) {
        this.company = company;
    }

    // ===== Excel ===================================================================================

    public Path exportExcel(ReportDocument report) throws IOException {
        Path output = output(report, ".xlsx");
        int columns = Math.max(4, report.columns().size());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(sheetName(report.title()));
            sheet.setRightToLeft(true);
            Styles styles = new Styles(workbook);
            int row = 0;
            row = mergedRow(sheet, row, columns, company.name(), styles.company, 26);
            row = mergedRow(sheet, row, columns, company.contactLine(), styles.contact, 18);
            row++;
            XSSFRow titleRow = sheet.createRow(row);
            titleRow.setHeightInPoints(22);
            int titleSpan = Math.max(1, columns / 2);
            for (int c = 0; c < columns; c++)
                titleRow.createCell(c).setCellStyle(c < titleSpan ? styles.title : styles.stamp);
            titleRow.getCell(0).setCellValue(report.title());
            titleRow.getCell(titleSpan).setCellValue("تاريخ التقرير: " + LocalDateTime.now().format(STAMP));
            sheet.addMergedRegion(new CellRangeAddress(row, row, 0, titleSpan - 1));
            if (columns - 1 > titleSpan) sheet.addMergedRegion(new CellRangeAddress(row, row, titleSpan, columns - 1));
            row += 2;
            row = fields(sheet, row, columns, report.info(), styles, false);
            if (!report.info().isEmpty()) row++;
            row = fields(sheet, row, columns, report.summary(), styles, true);
            if (!report.summary().isEmpty()) row++;

            int headerRow = row;
            XSSFRow header = sheet.createRow(row++);
            header.setHeightInPoints(20);
            for (int c = 0; c < report.columns().size(); c++) {
                XSSFCell cell = header.createCell(c);
                cell.setCellValue(report.columns().get(c).heading());
                cell.setCellStyle(styles.header);
            }
            for (int i = 0; i < report.rows().size(); i++) {
                XSSFRow line = sheet.createRow(row++);
                List<Object> values = report.rows().get(i);
                for (int c = 0; c < report.columns().size(); c++)
                    write(line.createCell(c), c < values.size() ? values.get(c) : null, report.columns().get(c).kind(),
                            styles, i % 2 == 1, false);
            }
            if (report.rows().isEmpty()) {
                XSSFRow empty = sheet.createRow(row);
                for (int c = 0; c < report.columns().size(); c++) empty.createCell(c).setCellStyle(styles.cell(Kind.CENTER, false, false));
                empty.getCell(0).setCellValue("لا توجد بيانات");
                if (report.columns().size() > 1)
                    sheet.addMergedRegion(new CellRangeAddress(row, row, 0, report.columns().size() - 1));
                row++;
            }
            if (!report.totals().isEmpty()) {
                XSSFRow totals = sheet.createRow(row++);
                for (int c = 0; c < report.columns().size(); c++)
                    write(totals.createCell(c), c < report.totals().size() ? report.totals().get(c) : null,
                            report.columns().get(c).kind(), styles, false, true);
            }
            float weights = 0;
            for (Column column : report.columns()) weights += column.weight();
            for (int c = 0; c < columns; c++) {
                float weight = c < report.columns().size() ? report.columns().get(c).weight() : 1;
                int chars = report.columns().isEmpty() ? 18 : Math.round(Math.max(9, weight / weights * 150));
                sheet.setColumnWidth(c, Math.min(255, chars) * 256);
            }
            sheet.createFreezePane(0, headerRow + 1);
            sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, -1, -1));
            PrintSetup print = sheet.getPrintSetup();
            print.setPaperSize(PrintSetup.A4_PAPERSIZE);
            print.setLandscape(landscape(report));
            sheet.setFitToPage(true);
            print.setFitWidth((short) 1);
            print.setFitHeight((short) 0);
            try (var stream = Files.newOutputStream(output)) {
                workbook.write(stream);
            }
        }
        return verify(output);
    }

    private static int mergedRow(XSSFSheet sheet, int row, int columns, String text, CellStyle style, float height) {
        XSSFRow r = sheet.createRow(row);
        r.setHeightInPoints(height);
        for (int c = 0; c < columns; c++) r.createCell(c).setCellStyle(style);
        r.getCell(0).setCellValue(text);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, columns - 1));
        return row + 1;
    }

    /** Label/value pairs, two pairs per row (label 1 column, value the rest of the half). */
    private static int fields(XSSFSheet sheet, int row, int columns, List<Field> fields, Styles styles, boolean bold) {
        int half = columns / 2;
        for (int i = 0; i < fields.size(); i += 2) {
            XSSFRow r = sheet.createRow(row);
            r.setHeightInPoints(18);
            for (int pair = 0; pair < 2 && i + pair < fields.size(); pair++) {
                Field field = fields.get(i + pair);
                int start = pair * half;
                int end = pair == 0 ? half - 1 : columns - 1;
                XSSFCell label = r.createCell(start);
                label.setCellValue(field.label());
                label.setCellStyle(styles.label);
                for (int c = start + 1; c <= end; c++)
                    write(r.createCell(c), c == start + 1 ? field.value() : null,
                            field.value() instanceof BigDecimal ? Kind.MONEY : Kind.TEXT, styles, false, bold);
                if (end > start + 1) sheet.addMergedRegion(new CellRangeAddress(row, row, start + 1, end));
            }
            row++;
        }
        return row;
    }

    private static void write(XSSFCell cell, Object value, Kind kind, Styles styles, boolean stripe, boolean bold) {
        if (value instanceof BigDecimal money) {
            cell.setCellValue(money.setScale(2, RoundingMode.HALF_UP).doubleValue());
            cell.setCellStyle(styles.cell(Kind.MONEY, stripe, bold));
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(styles.cell(Kind.NUMBER, stripe, bold));
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(styles.date(stripe));
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
            cell.setCellStyle(styles.cell(kind == Kind.MONEY || kind == Kind.NUMBER ? Kind.CENTER : kind, stripe, bold));
        }
    }

    private static final class Styles {
        final XSSFWorkbook workbook;
        final XSSFCellStyle company, contact, title, stamp, label, header;
        final Map<String, XSSFCellStyle> cells = new HashMap<>();

        Styles(XSSFWorkbook workbook) {
            this.workbook = workbook;
            company = style(16, true, NAVY, BAND, HorizontalAlignment.CENTER, false);
            contact = style(11, true, Color.BLACK, BAND, HorizontalAlignment.CENTER, false);
            title = style(12, true, Color.WHITE, ORANGE, HorizontalAlignment.CENTER, true);
            stamp = style(10, false, new Color(0x44, 0x4F, 0x5E), null, HorizontalAlignment.LEFT, false);
            label = style(11, true, Color.BLACK, GRAY, HorizontalAlignment.CENTER, true);
            header = style(11, true, Color.WHITE, NAVY, HorizontalAlignment.CENTER, true);
        }

        XSSFCellStyle cell(Kind kind, boolean stripe, boolean bold) {
            return cells.computeIfAbsent(kind + "/" + stripe + "/" + bold, key -> {
                XSSFCellStyle style = style(10, bold, Color.BLACK, bold ? TOTAL : stripe ? STRIPE : null,
                        kind == Kind.TEXT ? HorizontalAlignment.RIGHT : HorizontalAlignment.CENTER, true);
                if (kind == Kind.MONEY) style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
                if (kind == Kind.NUMBER) style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.##"));
                style.setWrapText(kind == Kind.TEXT);
                return style;
            });
        }

        XSSFCellStyle date(boolean stripe) {
            return cells.computeIfAbsent("date/" + stripe, key -> {
                XSSFCellStyle style = style(10, false, Color.BLACK, stripe ? STRIPE : null, HorizontalAlignment.CENTER, true);
                style.setDataFormat(workbook.createDataFormat().getFormat("yyyy/mm/dd"));
                return style;
            });
        }

        private XSSFCellStyle style(int size, boolean bold, Color color, Color fill, HorizontalAlignment align,
                boolean border) {
            XSSFCellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setColor(new XSSFColor(color, null));
            style.setFont(font);
            style.setAlignment(align);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if (fill != null) {
                style.setFillForegroundColor(new XSSFColor(fill, null));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (border) {
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
            }
            return style;
        }
    }

    // ===== PDF =====================================================================================

    private enum Align { RIGHT, CENTER, LEFT }

    private record Box(float x, float y, float w, float h, Color fill, boolean border, List<String> lines, float size,
            boolean bold, Color color, Align align) {
    }

    private static final float MARGIN = 28f;
    private static final float LINE = 1.25f;

    public Path exportPdf(ReportDocument report) throws IOException {
        Path output = output(report, ".pdf");
        PDRectangle size = landscape(report) ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;
        List<List<Box>> pages = layout(report, size.getWidth(), size.getHeight());
        try (PDDocument document = new PDDocument()) {
            PDFont regular = font(document, false);
            PDFont bold = font(document, true);
            Map<PDFont, Map<Integer, Boolean>> glyphs = new HashMap<>();
            for (int p = 0; p < pages.size(); p++) {
                List<Box> boxes = new ArrayList<>(pages.get(p));
                boxes.add(new Box(MARGIN, size.getHeight() - MARGIN + 6, size.getWidth() - 2 * MARGIN, 12, null, false,
                        List.of(company.name() + "  —  صفحة " + (p + 1) + " من " + pages.size()), 8, false,
                        new Color(0x6B, 0x76, 0x84), Align.CENTER));
                PDPage page = new PDPage(size);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float height = size.getHeight();
                    for (Box box : boxes) {
                        if (box.fill() == null) continue;
                        stream.setNonStrokingColor(box.fill());
                        stream.addRect(box.x(), height - box.y() - box.h(), box.w(), box.h());
                        stream.fill();
                    }
                    for (Box box : boxes) text(stream, box, box.bold() ? bold : regular, glyphs, height);
                    stream.setStrokingColor(new Color(0x55, 0x55, 0x55));
                    stream.setLineWidth(0.5f);
                    for (Box box : boxes) {
                        if (!box.border()) continue;
                        stream.addRect(box.x(), height - box.y() - box.h(), box.w(), box.h());
                        stream.stroke();
                    }
                }
            }
            document.getDocumentInformation().setTitle(report.title());
            document.getDocumentInformation().setCreator(company.name());
            document.save(output.toFile());
        }
        return verify(output);
    }

    private List<List<Box>> layout(ReportDocument report, float pageWidth, float pageHeight) {
        float width = pageWidth - 2 * MARGIN;
        float right = pageWidth - MARGIN;
        float bottom = pageHeight - MARGIN - 4;
        List<List<Box>> pages = new ArrayList<>();
        List<Box> page = new ArrayList<>();
        float y = band(page, right, width, MARGIN);
        // title (orange, right) and report time (left)
        float titleWidth = width * 0.55f;
        page.add(new Box(right - titleWidth, y, titleWidth, 22, ORANGE, true, List.of(report.title()), 11.5f, true,
                Color.WHITE, Align.CENTER));
        page.add(new Box(MARGIN, y, width - titleWidth - 8, 22, null, false,
                List.of("تاريخ التقرير: " + LocalDateTime.now().format(STAMP)), 9, false, new Color(0x44, 0x4F, 0x5E),
                Align.LEFT));
        y += 30;
        y = pdfFields(page, report.info(), right, width, y, false);
        if (!report.info().isEmpty()) y += 6;
        y = pdfFields(page, report.summary(), right, width, y, true);
        if (!report.summary().isEmpty()) y += 8;

        float[] widths = columnWidths(report.columns(), width);
        float headerHeight = 20;
        y = tableHeader(page, report.columns(), widths, right, y, headerHeight);
        List<List<Object>> rows = new ArrayList<>(report.rows());
        if (rows.isEmpty()) {
            page.add(new Box(MARGIN, y, width, 20, null, true, List.of("لا توجد بيانات"), 9, false, Color.DARK_GRAY,
                    Align.CENTER));
            y += 20;
        }
        for (int i = 0; i < rows.size(); i++) {
            List<Box> cells = new ArrayList<>();
            float h = tableRow(cells, report.columns(), rows.get(i), widths, right, y, i % 2 == 1 ? STRIPE : null,
                    false);
            if (y + h > bottom) {
                pages.add(page);
                page = new ArrayList<>();
                y = band(page, right, width, MARGIN);
                y = tableHeader(page, report.columns(), widths, right, y, headerHeight);
                cells.clear();
                h = tableRow(cells, report.columns(), rows.get(i), widths, right, y, i % 2 == 1 ? STRIPE : null, false);
            }
            page.addAll(cells);
            y += h;
        }
        if (!report.totals().isEmpty()) {
            List<Box> cells = new ArrayList<>();
            float h = tableRow(cells, report.columns(), report.totals(), widths, right, y, TOTAL, true);
            if (y + h > bottom) {
                pages.add(page);
                page = new ArrayList<>();
                y = band(page, right, width, MARGIN);
                cells.clear();
                h = tableRow(cells, report.columns(), report.totals(), widths, right, y, TOTAL, true);
            }
            page.addAll(cells);
        }
        pages.add(page);
        return pages;
    }

    private float band(List<Box> page, float right, float width, float y) {
        page.add(new Box(right - width, y, width, 24, BAND, false, List.of(company.name()), 15, true, NAVY,
                Align.CENTER));
        page.add(new Box(right - width, y + 24, width, 15, BAND, false, List.of(company.contactLine()), 9.5f, true,
                Color.BLACK, Align.CENTER));
        return y + 24 + 15 + 10;
    }

    private static float pdfFields(List<Box> page, List<Field> fields, float right, float width, float y, boolean bold) {
        float half = (width - 10) / 2;
        float labelWidth = Math.min(120, half * 0.42f);
        for (int i = 0; i < fields.size(); i += 2) {
            float rowHeight = 18;
            for (int pair = 0; pair < 2 && i + pair < fields.size(); pair++) {
                Field field = fields.get(i + pair);
                List<String> value = wrap(display(field.value()), half - labelWidth - 6, 9.5f, bold);
                rowHeight = Math.max(rowHeight, value.size() * 9.5f * LINE + 5);
            }
            for (int pair = 0; pair < 2 && i + pair < fields.size(); pair++) {
                Field field = fields.get(i + pair);
                float pairRight = right - pair * (half + 10);
                page.add(new Box(pairRight - labelWidth, y, labelWidth, rowHeight, GRAY, true, List.of(field.label()),
                        9.5f, true, Color.BLACK, Align.CENTER));
                boolean numeric = field.value() instanceof BigDecimal || field.value() instanceof Number;
                page.add(new Box(pairRight - half, y, half - labelWidth, rowHeight, null, true,
                        wrap(display(field.value()), half - labelWidth - 6, 9.5f, bold), 9.5f, bold, Color.BLACK,
                        numeric ? Align.CENTER : Align.RIGHT));
            }
            y += rowHeight;
        }
        return y;
    }

    private static float tableHeader(List<Box> page, List<Column> columns, float[] widths, float right, float y,
            float height) {
        float x = right;
        for (int c = 0; c < columns.size(); c++) {
            x -= widths[c];
            page.add(new Box(x, y, widths[c], height, NAVY, true, wrap(columns.get(c).heading(), widths[c] - 4, 9.5f,
                    true), 9.5f, true, Color.WHITE, Align.CENTER));
        }
        return y + height;
    }

    private static float tableRow(List<Box> cells, List<Column> columns, List<Object> values, float[] widths,
            float right, float y, Color fill, boolean bold) {
        float size = 9f;
        List<List<String>> texts = new ArrayList<>();
        float height = 18;
        for (int c = 0; c < columns.size(); c++) {
            Object value = c < values.size() ? values.get(c) : null;
            List<String> lines = columns.get(c).kind() == Kind.TEXT
                    ? wrap(display(value), widths[c] - 6, size, bold) : List.of(display(value));
            texts.add(lines);
            height = Math.max(height, lines.size() * size * LINE + 5);
        }
        float x = right;
        for (int c = 0; c < columns.size(); c++) {
            x -= widths[c];
            cells.add(new Box(x, y, widths[c], height, fill, true, texts.get(c), size, bold, Color.BLACK,
                    columns.get(c).kind() == Kind.TEXT ? Align.RIGHT : Align.CENTER));
        }
        return height;
    }

    private static float[] columnWidths(List<Column> columns, float width) {
        float total = 0;
        for (Column column : columns) total += column.weight();
        float[] widths = new float[columns.size()];
        for (int i = 0; i < widths.length; i++) widths[i] = width * columns.get(i).weight() / total;
        return widths;
    }

    private static void text(PDPageContentStream stream, Box box, PDFont font, Map<PDFont, Map<Integer, Boolean>> glyphs,
            float pageHeight) throws IOException {
        int count = box.lines().size();
        if (count == 0) return;
        float size = box.size();
        float available = box.w() - 6;
        String[] visual = new String[count];
        for (int i = 0; i < count; i++) {
            String line = box.lines().get(i);
            // Text without Arabic letters (phones, codes, Latin names) is already in reading order.
            visual[i] = supported(font, hasArabic(line) ? ArabicPdfText.visual(line) : line, glyphs);
            float width = font.getStringWidth(visual[i]) / 1000f * size;
            if (width > available && width > 0) size = size * available / width;
        }
        float lineHeight = size * LINE;
        float top = box.y() + (box.h() - count * lineHeight) / 2;
        stream.setNonStrokingColor(box.color());
        for (int i = 0; i < count; i++) {
            if (visual[i].isEmpty()) continue;
            float advance = font.getStringWidth(visual[i]) / 1000f * size;
            float x = switch (box.align()) {
                case RIGHT -> box.x() + box.w() - 3 - advance;
                case LEFT -> box.x() + 3;
                case CENTER -> box.x() + (box.w() - advance) / 2;
            };
            float baseline = top + i * lineHeight + size * 0.95f;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, pageHeight - baseline);
            stream.showText(visual[i]);
            stream.endText();
        }
    }

    private static boolean hasArabic(String text) {
        return text != null && text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC);
    }

    private static String supported(PDFont font, String text, Map<PDFont, Map<Integer, Boolean>> cache) {
        Map<Integer, Boolean> known = cache.computeIfAbsent(font, key -> new HashMap<>());
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            boolean ok = known.computeIfAbsent(cp, value -> {
                try {
                    font.encode(new String(Character.toChars(value)));
                    return true;
                } catch (IOException | IllegalArgumentException exception) {
                    return false;
                }
            });
            out.appendCodePoint(ok ? cp : '?');
        });
        return out.toString();
    }

    private static PDFont font(PDDocument document, boolean bold) throws IOException {
        Path file = InvoiceFonts.file(bold).or(() -> InvoiceFonts.file(false))
                .orElseThrow(() -> new IOException("لم يتم العثور على خط عربي (Arial أو Tahoma) في Windows"));
        return PDType0Font.load(document, file.toFile());
    }

    private static List<String> wrap(String text, float width, float size, boolean bold) {
        return InvoiceTemplateRenderer.wrap(text, Math.max(10, width), size, bold);
    }

    // ===== shared ==================================================================================

    /** Text shown for a value: money with thousands separators and two decimals, dates as yyyy/MM/dd. */
    public static String display(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal money)
            return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US))
                    .format(money.setScale(2, RoundingMode.HALF_UP));
        if (value instanceof Number number)
            return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(number);
        if (value instanceof LocalDate date) return DAY.format(date);
        return String.valueOf(value);
    }

    private static boolean landscape(ReportDocument report) {
        return report.columns().size() > 7;
    }

    private static Path output(ReportDocument report, String extension) throws IOException {
        return ExportFiles.path(report.fileName(), extension);
    }

    private static Path verify(Path output) throws IOException {
        if (!Files.isRegularFile(output) || Files.size(output) == 0) throw new IOException("لم يتم إنشاء الملف");
        return output;
    }

    private static String sheetName(String title) {
        String name = title == null || title.isBlank() ? "تقرير" : title.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return name.length() > 31 ? name.substring(0, 31) : name;
    }
}
