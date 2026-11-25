package com.cartonapp.ui;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.*;

/**
 * Date input (yyyy-MM-dd) with a calendar icon and a pop-up month calendar. Typing a date by hand
 * still works; an unreadable date shows a red outline.
 */
public final class DatePickerField extends JPanel {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو",
        "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    private static final String[] DAYS = {"س", "ح", "ن", "ث", "ر", "خ", "ج"}; // Saturday first

    private final JTextField input = new JTextField();
    private Runnable onChange = () -> { };
    private final java.util.List<Runnable> editListeners = new java.util.ArrayList<>();

    public DatePickerField(LocalDate value) {
        super(new BorderLayout());
        setOpaque(false);
        input.setFont(Theme.font(Font.BOLD, 12));
        input.setForeground(Theme.NAVY);
        input.setHorizontalAlignment(SwingConstants.CENTER);
        input.setPreferredSize(new Dimension(140, Theme.FIELD_HEIGHT));
        JButton open = new JButton(Icons.icon("calendar", 16, Theme.PRIMARY));
        open.setToolTipText("اختيار من التقويم");
        open.putClientProperty("JButton.buttonType", "toolBarButton");
        open.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        open.addActionListener(e -> showCalendar());
        input.putClientProperty("JTextField.trailingComponent", open);
        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { edited(); }
        });
        add(input);
        setDate(value);
    }

    /** Called after a date is picked from the calendar. */
    public void setOnChange(Runnable action) {
        onChange = action == null ? () -> { } : action;
    }

    public void setDate(LocalDate date) {
        input.setText(date == null ? "" : FORMAT.format(date));
    }

    /** The entered date, or null when the text is not a valid yyyy-MM-dd date. */
    public LocalDate date() {
        try {
            return LocalDate.parse(input.getText().trim(), FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /** Runs on every change, typed or picked (e.g. to recalculate something that depends on the date). */
    public void addEditListener(Runnable listener) {
        editListeners.add(listener);
    }

    private void edited() {
        validateText();
        editListeners.forEach(Runnable::run);
    }

    private void validateText() {
        input.putClientProperty("JComponent.outline", date() == null ? "error" : null);
    }

    private void showCalendar() {
        LocalDate current = date() == null ? LocalDate.now() : date();
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        YearMonth[] month = {YearMonth.from(current)};
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Theme.CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Runnable[] render = {null};
        render[0] = () -> {
            panel.removeAll();
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(true);
            head.setBackground(Theme.NAVY_2);
            head.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            JButton previous = nav("chevron", () -> { month[0] = month[0].minusMonths(1); render[0].run(); });
            JButton next = nav("chevron-left", () -> { month[0] = month[0].plusMonths(1); render[0].run(); });
            JLabel title = new JLabel(MONTHS[month[0].getMonthValue() - 1] + " " + month[0].getYear(),
                    SwingConstants.CENTER);
            title.setFont(Theme.font(Font.BOLD, 13));
            title.setForeground(Color.WHITE);
            head.add(previous, BorderLayout.EAST);
            head.add(title, BorderLayout.CENTER);
            head.add(next, BorderLayout.WEST);
            panel.add(head, BorderLayout.NORTH);
            JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
            grid.setOpaque(false);
            grid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            for (String day : DAYS) {
                JLabel label = new JLabel(day, SwingConstants.CENTER);
                label.setFont(Theme.font(Font.BOLD, 11));
                label.setForeground(Theme.MUTED_TEXT);
                grid.add(label);
            }
            LocalDate first = month[0].atDay(1);
            int offset = (first.getDayOfWeek().getValue() - DayOfWeek.SATURDAY.getValue() + 7) % 7;
            for (int i = 0; i < offset; i++) grid.add(new JLabel());
            for (int d = 1; d <= month[0].lengthOfMonth(); d++) {
                LocalDate day = month[0].atDay(d);
                JButton cell = new JButton(String.valueOf(d));
                cell.setFont(Theme.font(Font.PLAIN, 12));
                cell.setMargin(new Insets(4, 4, 4, 4));
                cell.setPreferredSize(new Dimension(34, 30));
                cell.setFocusPainted(false);
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (day.equals(current)) {
                    cell.setBackground(Theme.PRIMARY);
                    cell.setForeground(Color.WHITE);
                } else if (day.equals(LocalDate.now())) {
                    cell.setBackground(Theme.BABY_LIGHT);
                    cell.setForeground(Theme.PRIMARY_DARK);
                } else {
                    cell.setBackground(Color.WHITE);
                    cell.setForeground(Theme.NAVY);
                }
                cell.addActionListener(e -> {
                    setDate(day);
                    popup.setVisible(false);
                    onChange.run();
                });
                grid.add(cell);
            }
            panel.add(grid, BorderLayout.CENTER);
            JButton today = new JButton("اليوم");
            today.setFont(Theme.font(Font.BOLD, 11));
            today.putClientProperty("JButton.buttonType", "toolBarButton");
            today.setForeground(Theme.PRIMARY);
            today.addActionListener(e -> {
                setDate(LocalDate.now());
                popup.setVisible(false);
                onChange.run();
            });
            panel.add(today, BorderLayout.SOUTH);
            panel.revalidate();
            panel.repaint();
            popup.pack();
        };
        render[0].run();
        popup.add(panel);
        popup.show(input, 0, input.getHeight() + 2);
    }

    private static JButton nav(String icon, Runnable action) {
        JButton button = new JButton(Icons.icon(icon, 14, Color.WHITE));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.addActionListener(e -> action.run());
        return button;
    }
}
