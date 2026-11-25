package com.cartonapp.ui;

import javax.swing.Icon;

/** Chooses a field icon from its Arabic label, so every form gets consistent icons automatically. */
public final class FieldIcons {
    private static final String[][] RULES = {
        {"كلمة المرور", "lock"},
        {"الهاتف", "phone"},
        {"العنوان", "map-pin"},
        {"النشاط", "briefcase"},
        {"اسم الشركة", "building"},
        {"العميل", "user"},
        {"الاسم", "user"},
        {"اسم ", "user"},
        {"التاريخ", "calendar"},
        {"تاريخ", "calendar"},
        {"المرجع", "hash"},
        {"رقم", "hash"},
        {"الطول", "ruler"},
        {"العرض", "ruler"},
        {"الارتفاع", "ruler"},
        {"الطبقات", "layers"},
        {"ألوان", "palette"},
        {"نوع العلبة", "box"},
        {"الصنف", "box"},
        {"سعر", "tag"},
        {"الخصم", "tag"},
        {"المديونية", "coins"},
        {"المبلغ", "coins"},
        {"نقد", "wallet"},
        {"الكمية", "layers"},
    };

    private FieldIcons() { }

    /** Icon name for a label, or null when no icon fits. */
    public static String nameFor(String label) {
        if (label == null) return null;
        for (String[] rule : RULES) if (label.contains(rule[0])) return rule[1];
        return null;
    }

    public static Icon iconFor(String label, int size) {
        String name = nameFor(label);
        return name == null ? null : Icons.icon(name, size, Theme.PRIMARY);
    }
}
