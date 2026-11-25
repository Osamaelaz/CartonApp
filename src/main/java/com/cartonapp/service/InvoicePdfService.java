package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.service.InvoiceTemplateRenderer.Cell;
import com.cartonapp.service.InvoiceTemplateRenderer.Page;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * Writes one invoice as a real A4 PDF document (vector text, embedded Arial subset) from the same
 * template layout used by the preview and printer, so the PDF matches what the accountant sees.
 */
public final class InvoicePdfService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final InvoiceTemplateRenderer renderer = new InvoiceTemplateRenderer();

    /** Writes the invoice into the application's exports folder and returns the file. */
    public Path exportPdf(InvoiceDocument invoice, CompanyProfile company) throws IOException {
        Path output = ExportFiles.path(ExportFiles.invoiceName(
                invoice.customer() == null ? null : invoice.customer().name(), invoice.date()), ".pdf");
        write(invoice, company, output);
        return output;
    }

    public void write(InvoiceDocument invoice, CompanyProfile company, Path output) throws IOException {
        List<Page> pages = renderer.render(invoice, company);
        try (PDDocument document = new PDDocument()) {
            PDFont regular = load(document, false);
            PDFont bold = load(document, true);
            Glyphs glyphs = new Glyphs();
            for (Page page : pages) {
                PDPage pdfPage = new PDPage(PDRectangle.A4);
                document.addPage(pdfPage);
                try (PDPageContentStream stream = new PDPageContentStream(document, pdfPage)) {
                    for (Cell cell : page.cells()) {
                        if (cell.fill() == null) continue;
                        color(stream, cell.fill(), true);
                        stream.addRect(cell.x(), flip(cell.y() + cell.height()), cell.width(), cell.height());
                        stream.fill();
                    }
                    for (Cell cell : page.cells()) drawText(stream, cell, cell.bold() ? bold : regular, glyphs);
                    color(stream, Color.BLACK, false);
                    stream.setLineWidth(InvoiceTemplateRenderer.BORDER_WIDTH);
                    for (Cell cell : page.cells()) {
                        if (!cell.border()) continue;
                        stream.addRect(cell.x(), flip(cell.y() + cell.height()), cell.width(), cell.height());
                        stream.stroke();
                    }
                }
            }
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("فاتورة مبيعات " + invoice.number());
            info.setSubject(invoice.customer() == null ? "" : invoice.customer().name());
            info.setCreator(company.name());
            document.save(output.toFile());
        }
        if (!Files.isRegularFile(output) || Files.size(output) == 0)
            throw new IOException("لم يتم إنشاء ملف PDF");
    }

    private static void drawText(PDPageContentStream stream, Cell cell, PDFont font, Glyphs glyphs)
            throws IOException {
        int count = cell.lines().size();
        if (count == 0) return;
        float available = cell.width() - 2 * InvoiceTemplateRenderer.PADDING;
        float size = cell.fontSize();
        String[] visual = new String[count];
        for (int i = 0; i < count; i++) {
            visual[i] = glyphs.supported(font, ArabicPdfText.visual(cell.lines().get(i)));
            if (cell.shrink()) {
                float width = font.getStringWidth(visual[i]) / 1000f * size;
                if (width > available && width > 0) size = size * available / width;
            }
        }
        float lineHeight = size * InvoiceTemplateRenderer.LINE_FACTOR;
        float top = cell.y() + (cell.height() - count * lineHeight) / 2;
        color(stream, cell.color(), true);
        for (int i = 0; i < count; i++) {
            if (visual[i].isEmpty()) continue;
            float advance = font.getStringWidth(visual[i]) / 1000f * size;
            float x = switch (cell.align()) {
                case RIGHT -> cell.x() + cell.width() - InvoiceTemplateRenderer.PADDING - advance;
                case LEFT -> cell.x() + InvoiceTemplateRenderer.PADDING;
                case CENTER -> cell.x() + (cell.width() - advance) / 2;
            };
            float baseline = top + i * lineHeight + size * InvoiceTemplateRenderer.BASELINE_FACTOR;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, flip(baseline));
            stream.showText(visual[i]);
            stream.endText();
        }
    }

    private static float flip(float y) {
        return InvoiceTemplateRenderer.PAGE_HEIGHT - y;
    }

    private static void color(PDPageContentStream stream, Color color, boolean fill) throws IOException {
        if (fill) stream.setNonStrokingColor(color);
        else stream.setStrokingColor(color);
    }

    private static PDFont load(PDDocument document, boolean bold) throws IOException {
        Path file = InvoiceFonts.file(bold).or(() -> InvoiceFonts.file(false))
                .orElseThrow(() -> new IOException("لم يتم العثور على خط عربي (Arial أو Tahoma) في Windows"));
        return PDType0Font.load(document, file.toFile());
    }

    private static String safeFileName(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Replaces characters the embedded font cannot draw, instead of failing the whole export. */
    private static final class Glyphs {
        private final Map<PDFont, Map<Integer, Boolean>> cache = new HashMap<>();

        String supported(PDFont font, String text) {
            Map<Integer, Boolean> known = cache.computeIfAbsent(font, key -> new HashMap<>());
            StringBuilder out = new StringBuilder(text.length());
            text.codePoints().forEach(codePoint -> {
                boolean ok = known.computeIfAbsent(codePoint, cp -> {
                    try {
                        font.encode(new String(Character.toChars(cp)));
                        return true;
                    } catch (IOException | IllegalArgumentException exception) {
                        return false;
                    }
                });
                out.appendCodePoint(ok ? codePoint : '?');
            });
            return out.toString();
        }
    }
}
