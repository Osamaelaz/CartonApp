package com.cartonapp.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

/** Applies the common Make table treatment. */
public final class TableStyle {
    private TableStyle() { }

    public static void apply(JTable table) {
        table.setRowHeight(48);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.GRID);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setForeground(Theme.TEXT);
        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.TABLE_HEADER);
        header.setForeground(Theme.TABLE_HEADER_TEXT);
        header.setFont(Theme.font(Font.BOLD, 12));
        header.setPreferredSize(new java.awt.Dimension(1, 40));
        header.setReorderingAllowed(false);
    }
}
