package com.cartonapp.service;

import com.cartonapp.model.Customer;
import com.cartonapp.model.CustomerAccount;
import com.cartonapp.model.CustomerAccountRow;
import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.ProductSale;
import com.cartonapp.model.ProductSummary;
import com.cartonapp.model.StatementEntry;
import com.cartonapp.service.ReportDocument.Column;
import com.cartonapp.service.ReportDocument.Field;
import com.cartonapp.service.ReportDocument.Kind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the export reports from the same service data the screens show, so exported numbers
 * always match the application: customer list, one customer's account, product list, one product.
 */
public final class ReportService {
    private final CustomerService customers;
    private final ProductService products;
    private final InvoiceService invoices;

    public ReportService(CustomerService customers, ProductService products, InvoiceService invoices) {
        this.customers = customers;
        this.products = products;
        this.invoices = invoices;
    }

    /** All customers with opening debt, invoices, collections and current debt. */
    public ReportDocument customerList() throws Exception {
        List<CustomerAccountRow> accounts = customers.findAllAccounts();
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal opening = BigDecimal.ZERO, invoiced = BigDecimal.ZERO, collected = BigDecimal.ZERO,
                balance = BigDecimal.ZERO;
        int serial = 1;
        for (CustomerAccountRow row : accounts) {
            Customer c = row.customer();
            CustomerAccount a = row.account();
            rows.add(Arrays.asList(serial++, c.code(), c.name(), c.industry(), c.phone(), c.address(),
                    a.openingBalance(), a.invoicesTotal(), a.collected(), a.balance()));
            opening = opening.add(a.openingBalance());
            invoiced = invoiced.add(a.invoicesTotal());
            collected = collected.add(a.collected());
            balance = balance.add(a.balance());
        }
        long debtors = accounts.stream().filter(r -> r.account().balance().signum() > 0).count();
        return new ReportDocument("العملاء " + ExportFiles.date(null), "تقرير العملاء", null, List.of(),
                List.of(new Field("عدد العملاء", accounts.size()), new Field("العملاء المدينون", debtors),
                        new Field("إجمالي الأرصدة الافتتاحية", opening), new Field("إجمالي الفواتير", invoiced),
                        new Field("إجمالي المحصّل", collected), new Field("إجمالي المديونية", balance)),
                List.of(new Column("م", Kind.CENTER, 4), new Column("الكود", Kind.CENTER, 7),
                        new Column("اسم العميل", Kind.TEXT, 17), new Column("النشاط", Kind.TEXT, 11),
                        new Column("الهاتف", Kind.CENTER, 11), new Column("العنوان", Kind.TEXT, 18),
                        new Column("الرصيد الافتتاحي", Kind.MONEY, 11), new Column("إجمالي الفواتير", Kind.MONEY, 11),
                        new Column("المحصّل", Kind.MONEY, 11), new Column("المديونية", Kind.MONEY, 11)),
                rows, Arrays.asList("", "", "الإجمالي", "", "", "", opening, invoiced, collected, balance));
    }

    /** One customer: contact data, account totals and the full statement (opening debt first). */
    public ReportDocument customerAccount(String customerCode) throws Exception {
        Customer c = customers.findByCode(customerCode).orElseThrow(() -> new IllegalArgumentException("العميل غير موجود"));
        CustomerAccount a = customers.findAccount(customerCode);
        List<StatementEntry> statement = invoices.findCustomerStatement(customerCode);
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        for (StatementEntry e : statement) {
            rows.add(Arrays.asList(e.date(), e.description(), e.invoiceNumber(), e.debit(), e.credit(), e.balance()));
            debit = debit.add(e.debit());
            credit = credit.add(e.credit());
        }
        BigDecimal closing = statement.isEmpty() ? a.balance() : statement.getLast().balance();
        return new ReportDocument(ExportFiles.statementName(c.name(), null), "كشف حساب " + c.name(), c.name(),
                List.of(new Field("العميل", c.name()), new Field("الكود", c.code()),
                        new Field("النشاط", nz(c.industry())), new Field("الهاتف", nz(c.phone())),
                        new Field("العنوان", nz(c.address()))),
                List.of(new Field("الرصيد الافتتاحي", a.openingBalance()), new Field("إجمالي الفواتير", a.invoicesTotal()),
                        new Field("المحصّل", a.collected()), new Field("المديونية", a.balance())),
                List.of(new Column("التاريخ", Kind.CENTER, 11), new Column("البيان", Kind.TEXT, 30),
                        new Column("رقم الفاتورة", Kind.CENTER, 15), new Column("مدين", Kind.MONEY, 14),
                        new Column("دائن", Kind.MONEY, 14), new Column("الرصيد", Kind.MONEY, 16)),
                rows, Arrays.asList("", "الإجمالي", "", debit, credit, closing));
    }

    /** All products with their customer, specification and current carton price. */
    public ReportDocument productList() throws Exception {
        List<ProductSummary> all = products.findAll();
        List<List<Object>> rows = new ArrayList<>();
        int serial = 1;
        for (ProductSummary p : all)
            rows.add(Arrays.asList(serial++, p.code(), p.name(), p.customerName(), p.boxType(), p.dimensions(),
                    p.layerCount(), p.printColors(), p.currentPrice()));
        long customersCount = all.stream().map(ProductSummary::customerCode).distinct().count();
        return new ReportDocument("المنتجات " + ExportFiles.date(null), "تقرير المنتجات", null, List.of(),
                List.of(new Field("عدد المنتجات", all.size()), new Field("عدد العملاء", customersCount)),
                List.of(new Column("م", Kind.CENTER, 4), new Column("الكود", Kind.CENTER, 8),
                        new Column("اسم المنتج", Kind.TEXT, 20), new Column("العميل", Kind.TEXT, 17),
                        new Column("نوع العلبة", Kind.TEXT, 14), new Column("المقاس", Kind.CENTER, 14),
                        new Column("الطبقات", Kind.CENTER, 7), new Column("الألوان", Kind.CENTER, 7),
                        new Column("سعر الكرتونة", Kind.MONEY, 10)),
                rows, List.of());
    }

    /** One product: specification, current price and every posted invoice line that sold it. */
    public ReportDocument product(String productCode) throws Exception {
        ProductSummary p = products.findByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("المنتج غير موجود"));
        List<ProductSale> sales = products.findSales(p.id());
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal quantity = BigDecimal.ZERO, value = BigDecimal.ZERO, discount = BigDecimal.ZERO,
                net = BigDecimal.ZERO;
        for (ProductSale s : sales) {
            InvoiceLine line = new InvoiceLine(0, 0, p.id(), s.quantity(), "", s.unitPrice(), s.discount());
            BigDecimal lineValue = InvoiceCalculator.lineValue(line);
            BigDecimal lineNet = InvoiceCalculator.lineNet(line);
            rows.add(Arrays.asList(s.invoiceNumber(), s.date(), s.customerName(), s.quantity().doubleValue(), s.unitPrice(),
                    lineValue, s.discount(), lineNet));
            quantity = quantity.add(s.quantity());
            value = value.add(lineValue);
            discount = discount.add(s.discount());
            net = net.add(lineNet);
        }
        BigDecimal sheetLength = p.lengthCm().add(p.widthCm()).multiply(BigDecimal.valueOf(2));
        BigDecimal sheetWidth = p.widthCm().add(p.heightCm());
        return new ReportDocument(ExportFiles.productName(p.name(), p.customerName()),
                p.name() + " - " + p.customerName(), p.name(),
                List.of(new Field("المنتج", p.name()), new Field("الكود", p.code()),
                        new Field("العميل", p.customerName()), new Field("نوع العلبة", nz(p.boxType())),
                        new Field("المقاس", p.dimensions()), new Field("عدد الطبقات", p.layerCount()),
                        new Field("ألوان الطباعة", p.printColors()),
                        new Field("مقاس الشيت (محسوب)", plain(sheetLength) + " × " + plain(sheetWidth) + " سم")),
                List.of(new Field("سعر الكرتونة الحالي", p.currentPrice()), new Field("عدد مرات البيع", sales.size()),
                        new Field("إجمالي الكمية المباعة", quantity.doubleValue()), new Field("صافي المبيعات", net)),
                List.of(new Column("رقم الفاتورة", Kind.CENTER, 13), new Column("التاريخ", Kind.CENTER, 11),
                        new Column("العميل", Kind.TEXT, 20), new Column("الكمية", Kind.NUMBER, 10),
                        new Column("الوحدة", Kind.MONEY, 10), new Column("القيمة", Kind.MONEY, 12),
                        new Column("قيمة الخصم", Kind.MONEY, 11), new Column("الصافي", Kind.MONEY, 12)),
                rows, Arrays.asList("الإجمالي", "", "", quantity.doubleValue(), "", value, discount, net));
    }

    private static String nz(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
