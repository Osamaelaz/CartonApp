package com.cartonapp.ui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

/** Applies the application's Arabic right-to-left reading direction recursively. */
public final class Rtl {
    private Rtl() { }

    public static <T extends Component> T apply(T component) {
        component.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        if (component instanceof JTextComponent text) {
            text.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            if (text instanceof JTextField field) field.setHorizontalAlignment(SwingConstants.RIGHT);
            if (text instanceof JTextArea area) area.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else if (component instanceof JComboBox<?> combo) {
            combo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            combo.setRenderer(new BasicComboBoxRenderer() {{
                setHorizontalAlignment(SwingConstants.RIGHT);
                setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            }});
        } else if (component instanceof JList<?> list) {
            list.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else if (component instanceof JLabel label) {
            label.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        if (component instanceof JTable table) {
            table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            DefaultTableCellRenderer cells = new DefaultTableCellRenderer();
            cells.setHorizontalAlignment(SwingConstants.RIGHT);
            cells.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            table.setDefaultRenderer(Object.class, cells);
            JTableHeader header = table.getTableHeader();
            header.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.RIGHT);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) apply(child);
        }
        return component;
    }
}
