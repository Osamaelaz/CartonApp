package com.cartonapp.service;

import com.cartonapp.model.InvoiceLine;
import com.cartonapp.model.InvoiceTotals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Sales-invoice calculations from the official Excel template, using BigDecimal with two decimals
 * (half-up), the same rounding the database queries use (PostgreSQL ROUND):
 * <ul>
 * <li>القيمة = الكمية × الوحدة (unit price)</li>
 * <li>الصافي = القيمة − قيمة الخصم</li>
 * <li>قيمة الفاتورة = Σ الصافي; إجمالي الكميات = Σ الكمية</li>
 * <li>إجمالي الفاتورة والمديونية = قيمة الفاتورة + الرصيد السابق − خصم إضافي</li>
 * <li>الرصيد النهائي = إجمالي الفاتورة والمديونية − المدفوع نقداً</li>
 * </ul>
 */
public final class InvoiceCalculator {
    private InvoiceCalculator() { }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /** القيمة: quantity × unit price, rounded to 2 decimals. */
    public static BigDecimal lineValue(InvoiceLine line) {
        return money(line.quantity().multiply(line.unitPrice()));
    }

    /** الصافي: line value − line discount. */
    public static BigDecimal lineNet(InvoiceLine line) {
        return lineValue(line).subtract(money(line.discount()));
    }

    /** قيمة الفاتورة: sum of the line net amounts (line discounts already applied once). */
    public static BigDecimal invoiceValue(List<InvoiceLine> lines) {
        return lines.stream().map(InvoiceCalculator::lineNet).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public static BigDecimal totalQuantity(List<InvoiceLine> lines) {
        return lines.stream().map(InvoiceLine::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Amount the invoice adds to the customer's debt: invoice value − additional discount. */
    public static BigDecimal invoiceDue(List<InvoiceLine> lines, BigDecimal additionalDiscount) {
        return invoiceValue(lines).subtract(money(additionalDiscount));
    }

    public static InvoiceTotals totals(List<InvoiceLine> lines, BigDecimal previousBalance,
            BigDecimal additionalDiscount, BigDecimal cashPaid) {
        BigDecimal value = invoiceValue(lines);
        BigDecimal previous = money(previousBalance);
        BigDecimal discount = money(additionalDiscount);
        BigDecimal cash = money(cashPaid);
        BigDecimal withDebt = value.add(previous).subtract(discount);
        return new InvoiceTotals(totalQuantity(lines), value, previous, discount, withDebt, cash,
                withDebt.subtract(cash));
    }
}
