package com.cartonapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Shared RTL pagination controls for every paged table. */
public final class PaginationBar extends JPanel {
    public static final int DEFAULT_PAGE_SIZE = 6;
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
    private final JLabel range = new JLabel();
    private final JLabel pageSize = new JLabel(DEFAULT_PAGE_SIZE + " سجلات في الصفحة");
    private IntConsumer pageListener = page -> { };
    private int totalRecords;
    private int currentPage;

    public PaginationBar() {
        super(new BorderLayout());
        setBackground(Theme.BABY_SOFT);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        buttons.setOpaque(false);
        buttons.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);
        range.setHorizontalAlignment(JLabel.RIGHT);
        pageSize.setHorizontalAlignment(JLabel.LEFT);
        styleLabel(range);
        styleLabel(pageSize);
        add(range, BorderLayout.EAST);
        add(buttons, BorderLayout.CENTER);
        add(pageSize, BorderLayout.WEST);
        update(0, 0);
    }

    public void setPageListener(IntConsumer listener) {
        pageListener = listener == null ? page -> { } : listener;
    }

    public int getPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    public void update(int totalRecords, int zeroBasedPage) {
        this.totalRecords = Math.max(0, totalRecords);
        int pageCount = pageCount();
        currentPage = this.totalRecords == 0 ? 0 : Math.max(0, Math.min(zeroBasedPage, pageCount - 1));
        int start = this.totalRecords == 0 ? 0 : currentPage * DEFAULT_PAGE_SIZE + 1;
        int end = this.totalRecords == 0 ? 0 : Math.min((currentPage + 1) * DEFAULT_PAGE_SIZE, this.totalRecords);
        range.setText("عرض " + start + "–" + end + " من " + this.totalRecords);
        buttons.removeAll();
        if (this.totalRecords > 0) {
            JButton previous = button(Icons.icon("chevron", 14, Theme.NAVY), "الصفحة السابقة");
            previous.setEnabled(currentPage > 0);
            previous.addActionListener(event -> pageListener.accept(currentPage - 1));
            buttons.add(previous);
            for (int page = 0; page < pageCount; page++) {
                final int selected = page;
                JButton number = button(Integer.toString(page + 1), null);
                if (page == currentPage) {
                    number = new AppButton(Integer.toString(page + 1), AppButton.Kind.PRIMARY);
                    number.setPreferredSize(new java.awt.Dimension(34, 32));
                    number.setMargin(new java.awt.Insets(0, 0, 0, 0));
                }
                number.addActionListener(event -> pageListener.accept(selected));
                buttons.add(number);
            }
            JButton next = button(Icons.icon("chevron-left", 14, Theme.NAVY), "الصفحة التالية");
            next.setEnabled(currentPage < pageCount - 1);
            next.addActionListener(event -> pageListener.accept(currentPage + 1));
            buttons.add(next);
        }
        buttons.revalidate();
        buttons.repaint();
    }

    private int pageCount() {
        return Math.max(1, (totalRecords + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE);
    }

    private JButton button(String text, String tooltip) {
        JButton button = new JButton(text);
        if (tooltip != null) {
            button.setToolTipText(tooltip);
        }
        return styleButton(button);
    }

    private JButton button(javax.swing.Icon icon, String tooltip) {
        JButton button = button("", tooltip);
        button.setIcon(icon);
        return button;
    }

    private static JButton styleButton(JButton button) {
        button.setFont(Theme.font(Font.PLAIN, 11));
        button.setPreferredSize(new java.awt.Dimension(30, 30));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setBackground(Theme.CARD);
        button.setForeground(Theme.NAVY);
        return button;
    }

    private static void styleLabel(JLabel label) {
        label.setFont(Theme.font(Font.PLAIN, 11));
        label.setForeground(Theme.MUTED_TEXT);
    }
}
