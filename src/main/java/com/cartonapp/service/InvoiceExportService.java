package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.model.InvoiceDocument;
import com.cartonapp.model.InvoiceDocumentLine;
import com.cartonapp.model.InvoiceLine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Exports one invoice into a copy of the official Excel template (bundled as
 * {@code /templates/sales-invoice.xlsx}), keeping its cells, styles, merges and formulas. Invoices
 * with more than ten lines get extra rows styled like the template's line rows.
 */
public final class InvoiceExportService {
    private static final String TEMPLATE = "/templates/sales-invoice.xlsx";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int FIRST_LINE_ROW = 10;   // Excel row 11
    private static final int TEMPLATE_LINE_ROWS = 10;
    private static final int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6;

    public Path exportExcel(InvoiceDocument invoice, CompanyProfile company) throws IOException {
        Path output = ExportFiles.path(ExportFiles.invoiceName(
                invoice.customer() == null ? null : invoice.customer().name(), invoice.date()), ".xlsx");
        try (InputStream template = InvoiceExportService.class.getResourceAsStream(TEMPLATE)) {
            if (template == null) throw new IOException("قالب الفاتورة غير موجود داخل البرنامج");
            try (XSSFWorkbook workbook = new XSSFWorkbook(template)) {
                fill(workbook.getSheetAt(0), invoice, company);
                workbook.setForceFormulaRecalculation(true);
                XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
                try (var stream = Files.newOutputStream(output)) {
                    workbook.write(stream);
                }
            }
        }
        if (!Files.isRegularFile(output) || Files.size(output) == 0) throw new IOException("لم يتم إنشاء ملف Excel");
        return output;
    }

    private static void fill(XSSFSheet sheet, InvoiceDocument invoice, CompanyProfile company) {
        int lineCount = invoice.lines().size();
        int rows = Math.max(lineCount, TEMPLATE_LINE_ROWS);
        int extra = rows - TEMPLATE_LINE_ROWS;

        // Drop the template's shared line formulas and the TODAY() date (children before masters),
        // then rewrite them explicitly so shifting rows cannot leave broken shared-formula references.
        for (int r = FIRST_LINE_ROW + TEMPLATE_LINE_ROWS - 1; r >= FIRST_LINE_ROW; r--) {
            removeFormula(sheet, r, E);
            removeFormula(sheet, r, G);
        }
        removeFormula(sheet, 3, G);
        removeFormula(sheet, 21, C);
        removeFormula(sheet, 21, G);
        removeFormula(sheet, 22, B);
        removeFormula(sheet, 24, G);

        if (extra > 0) {
            int firstAfterLines = FIRST_LINE_ROW + TEMPLATE_LINE_ROWS; // Excel row 21
            sheet.shiftRows(firstAfterLines, sheet.getLastRowNum(), extra, true, false);
            // POI moves the acknowledgement merge (A25:E26) but leaves a stale mergeCells count, which
            // Excel reports as a damaged file; keep the count equal to the actual merges.
            var merges = sheet.getCTWorksheet().getMergeCells();
            if (merges != null) merges.setCount(merges.sizeOfMergeCellArray());
            XSSFRow model = sheet.getRow(firstAfterLines - 1);
            for (int r = firstAfterLines; r < firstAfterLines + extra; r++) {
                XSSFRow row = sheet.createRow(r);
                row.setHeight(model.getHeight());
                for (int c = A; c <= G; c++) row.createCell(c).setCellStyle(model.getCell(c).getCellStyle());
            }
        }

        // Rows 1-2: company; row 4: number and date; rows 6-8: customer.
        cell(sheet, 0, A).setCellValue(company.name());
        cell(sheet, 1, A).setCellValue(company.contactLine());
        cell(sheet, 3, E).setCellValue(invoice.number());
        // The template's number cell wraps; application numbers (INV-nnnnn) are longer than its
        // sample "1", so shrink the text to fit instead of growing row 4.
        var numberStyle = sheet.getWorkbook().createCellStyle();
        numberStyle.cloneStyleFrom(cell(sheet, 3, E).getCellStyle());
        numberStyle.setWrapText(false);
        numberStyle.setShrinkToFit(true);
        cell(sheet, 3, E).setCellStyle(numberStyle);
        if (invoice.date() != null) cell(sheet, 3, G).setCellValue(invoice.date());
        String name = invoice.customer() == null ? "" : invoice.customer().name();
        String address = invoice.customer() == null ? "" : invoice.customer().address();
        cell(sheet, 5, B).setCellValue(name);
        cell(sheet, 6, B).setCellValue(address == null ? "" : address);
        cell(sheet, 7, B).setCellValue(name);

        // Lines: values entered as numbers, القيمة and الصافي as formulas like the template.
        for (int i = 0; i < rows; i++) {
            int r = FIRST_LINE_ROW + i;
            int excelRow = r + 1;
            cell(sheet, r, A).setCellValue(i + 1);
            if (i < lineCount) {
                InvoiceDocumentLine documentLine = invoice.lines().get(i);
                InvoiceLine line = documentLine.line();
                cell(sheet, r, B).setCellValue(documentLine.itemName());
                cell(sheet, r, C).setCellValue(line.quantity().doubleValue());
                cell(sheet, r, D).setCellValue(InvoiceCalculator.money(line.unitPrice()).doubleValue());
                if (line.discount().signum() != 0)
                    cell(sheet, r, F).setCellValue(InvoiceCalculator.money(line.discount()).doubleValue());
            }
            // ROUND keeps Excel's value identical to the application's two-decimal line value.
            cell(sheet, r, E).setCellFormula("IF(OR(C" + excelRow + "=\"\",D" + excelRow + "=\"\"),\"\",ROUND(C"
                    + excelRow + "*D" + excelRow + ",2))");
            // Same result as the template's E-N(F); written without N() so POI can store computed values
            // and the file shows correct numbers even in viewers that do not recalculate.
            cell(sheet, r, G).setCellFormula("IF(E" + excelRow + "=\"\",\"\",E" + excelRow + "-IF(ISNUMBER(F" + excelRow
                    + "),F" + excelRow + ",0))");
        }

        int lastLine = FIRST_LINE_ROW + rows; // Excel row number of the last line
        int r22 = 21 + extra, r23 = 22 + extra, r24 = 23 + extra, r25 = 24 + extra;
        cell(sheet, r22, B).setCellValue(InvoiceCalculator.money(invoice.cashPaid()).doubleValue());
        cell(sheet, r22, C).setCellFormula("SUM(C11:C" + lastLine + ")");
        if (InvoiceCalculator.totalQuantity(invoice.invoiceLines()).stripTrailingZeros().scale() > 0) {
            // The template shows the quantity total as #,##0; keep fractions visible so Excel agrees with the PDF.
            var style = sheet.getWorkbook().createCellStyle();
            style.cloneStyleFrom(cell(sheet, r22, C).getCellStyle());
            style.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("#,##0.00"));
            cell(sheet, r22, C).setCellStyle(style);
        }
        cell(sheet, r22, G).setCellFormula("SUM(G11:G" + lastLine + ")");
        cell(sheet, r23, B).setCellFormula("G" + (r25 + 1) + "-B" + (r22 + 1));
        cell(sheet, r23, G).setCellValue(InvoiceCalculator.money(invoice.previousBalance()).doubleValue());
        cell(sheet, r24, G).setCellValue(InvoiceCalculator.money(invoice.additionalDiscount()).doubleValue());
        cell(sheet, r25, G).setCellFormula("G" + (r22 + 1) + "+G" + (r23 + 1) + "-G" + (r24 + 1));

        // Print on A4, one page wide, so Excel's own printing matches the application's PDF.
        sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.getPrintSetup().setLandscape(false);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private static void removeFormula(XSSFSheet sheet, int row, int column) {
        XSSFRow r = sheet.getRow(row);
        if (r == null) return;
        XSSFCell c = r.getCell(column);
        if (c != null && c.getCellType() == CellType.FORMULA) c.removeFormula();
    }

    private static XSSFCell cell(XSSFSheet sheet, int row, int column) {
        XSSFRow r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);
        XSSFCell c = r.getCell(column);
        return c == null ? r.createCell(column) : c;
    }

    private static String safeFileName(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
