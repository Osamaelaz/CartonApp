package com.cartonapp.service;

import com.cartonapp.model.InvoiceInput;
import com.cartonapp.model.InvoiceLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Validation rules applied before an invoice is created or updated. Messages are Arabic. */
public final class InvoiceValidationService {
    private static final int MONEY_SCALE = 2;

    public List<String> validate(InvoiceInput input) {
        List<String> problems = new ArrayList<>();
        if (input.customerCode() == null || input.customerCode().isBlank())
            problems.add("اختر العميل.");
        if (input.date() == null)
            problems.add("أدخل تاريخ فاتورة صحيحًا.");
        if (input.lines().isEmpty())
            problems.add("أضف صنفًا واحدًا على الأقل.");
        boolean linesValid = true;
        for (int i = 0; i < input.lines().size(); i++) {
            InvoiceLine line = input.lines().get(i);
            String prefix = "البند " + (i + 1) + ": ";
            int before = problems.size();
            if (line.productId() == null)
                problems.add(prefix + "اختر الصنف.");
            if (line.quantity().signum() <= 0)
                problems.add(prefix + "الكمية يجب أن تكون أكبر من صفر.");
            else if (tooManyDecimals(line.quantity()))
                problems.add(prefix + "الكمية بحد أقصى منزلتين عشريتين.");
            if (line.unitPrice().signum() < 0)
                problems.add(prefix + "سعر الوحدة لا يمكن أن يكون سالبًا.");
            else if (tooManyDecimals(line.unitPrice()))
                problems.add(prefix + "سعر الوحدة بحد أقصى منزلتين عشريتين.");
            if (line.discount().signum() < 0)
                problems.add(prefix + "قيمة الخصم لا يمكن أن تكون سالبة.");
            else if (tooManyDecimals(line.discount()))
                problems.add(prefix + "قيمة الخصم بحد أقصى منزلتين عشريتين.");
            if (problems.size() == before && line.discount().compareTo(InvoiceCalculator.lineValue(line)) > 0)
                problems.add(prefix + "قيمة الخصم لا يمكن أن تتجاوز قيمة الصنف.");
            linesValid &= problems.size() == before;
        }
        BigDecimal additional = input.additionalDiscount();
        if (additional.signum() < 0)
            problems.add("الخصم الإضافي لا يمكن أن يكون سالبًا.");
        else if (tooManyDecimals(additional))
            problems.add("الخصم الإضافي بحد أقصى منزلتين عشريتين.");
        else if (linesValid && !input.lines().isEmpty()
                && additional.compareTo(InvoiceCalculator.invoiceValue(input.lines())) > 0)
            problems.add("الخصم الإضافي لا يمكن أن يتجاوز قيمة الفاتورة.");
        if (input.cashPaid().signum() < 0)
            problems.add("المدفوع نقدًا لا يمكن أن يكون سالبًا.");
        else if (tooManyDecimals(input.cashPaid()))
            problems.add("المدفوع نقدًا بحد أقصى منزلتين عشريتين.");
        return problems;
    }

    public void requireValid(InvoiceInput input) throws InvoiceValidationException {
        List<String> problems = validate(input);
        if (!problems.isEmpty()) throw new InvoiceValidationException(problems);
    }

    private static boolean tooManyDecimals(BigDecimal value) {
        return value.stripTrailingZeros().scale() > MONEY_SCALE;
    }
}
