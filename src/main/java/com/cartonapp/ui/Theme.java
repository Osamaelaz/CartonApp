package com.cartonapp.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Shared visual tokens. Brand palette: deep navy for structure (top bar, titles, table text),
 * baby blue for surfaces and highlights, a clear blue for actions, plus green/orange/red for status.
 */
public final class Theme {
    // Brand
    public static final Color NAVY = new Color(0x0A, 0x25, 0x40);
    public static final Color NAVY_2 = new Color(0x15, 0x3E, 0x6B);
    public static final Color NAVY_3 = new Color(0x1F, 0x4E, 0x85);
    public static final Color PRIMARY = new Color(0x1E, 0x6F, 0xD9);
    public static final Color PRIMARY_DARK = new Color(0x15, 0x58, 0xB0);
    public static final Color BABY = new Color(0x8C, 0xC8, 0xF5);
    public static final Color BABY_LIGHT = new Color(0xE3, 0xF0, 0xFD);
    public static final Color BABY_SOFT = new Color(0xF3, 0xF8, 0xFE);
    public static final Color NAV_TEXT = new Color(0xC3, 0xD9, 0xF0);

    // Surfaces and text
    /** Page background: soft blue-gray. */
    public static final Color CANVAS = new Color(0xD8, 0xE6, 0xF6);
    /** Lower end of the page gradient. */
    public static final Color CANVAS_2 = new Color(0xEA, 0xF2, 0xFC);
    /** Main surface for cards, dialogs and tables (replaces plain white). */
    public static final Color CARD = new Color(0xEF, 0xF5, 0xFD);
    /** Header band of cards and dialog sections. */
    public static final Color SECTION = new Color(0xDC, 0xEB, 0xFB);
    /** Input fields stay brighter than the surface so they read as editable. */
    public static final Color FIELD = new Color(0xFF, 0xFF, 0xFF);
    public static final Color TEXT = new Color(0x0F, 0x21, 0x3D);
    public static final Color MUTED_TEXT = new Color(0x3A, 0x4C, 0x66);
    public static final Color BORDER = new Color(0xB3, 0xCB, 0xE8);
    public static final Color FIELD_BORDER = new Color(0xB8, 0xCC, 0xE2);
    public static final Color LABEL_TEXT = new Color(0x1F, 0x35, 0x55);
    public static final Color FIELD_ICON = new Color(0x4F, 0x69, 0x8A);
    public static final Color NOTE_TEXT = new Color(0x4C, 0x5E, 0x78);

    // Tables
    public static final Color TABLE_HEADER = NAVY_2;
    public static final Color TABLE_HEADER_TEXT = Color.WHITE;
    /** Every second row, for easy reading across wide tables. */
    public static final Color ROW_ALT = new Color(0xE4, 0xEE, 0xFB);
    public static final Color ROW_HOVER = new Color(0xD3, 0xE6, 0xFA);
    public static final Color SELECTION = new Color(0xC4, 0xDD, 0xF8);
    public static final Color GRID = new Color(0xD0, 0xDF, 0xF0);

    // Status
    public static final Color SUCCESS = new Color(0x16, 0x8A, 0x5B);
    public static final Color WARNING = new Color(0xE0, 0x8A, 0x1E);
    public static final Color DANGER = new Color(0xD1, 0x43, 0x43);
    public static final Color ERROR = new Color(0xB7, 0x2C, 0x35);

    // Product price card (kept names, brand colours)
    public static final Color PRICE_HEAD_START = BABY_LIGHT;
    public static final Color PRICE_HEAD_END = CARD;
    public static final Color PRICE_DIVIDER = new Color(0xE9, 0xF1, 0xF9);
    public static final Color PRICE_VALUE = PRIMARY_DARK;
    public static final Color PRICE_LABEL = MUTED_TEXT;
    public static final Color PRICE_ROW_TEXT = new Color(0x2C, 0x40, 0x5C);
    public static final Color CALCULATED_FIELD = BABY_SOFT;
    public static final Color CALCULATED_ACCENT = BABY;

    public static final int CARD_WIDTH = 390;
    public static final int CARD_RADIUS = 12;
    public static final int CARD_PADDING_TOP = 34;
    public static final int CARD_PADDING_HORIZONTAL = 36;
    public static final int CARD_PADDING_BOTTOM = 29;
    public static final int LOGO_SIZE = 64;
    public static final int LOGO_RADIUS = 8;
    public static final int FIELD_HEIGHT = 34;
    public static final int BUTTON_HEIGHT = 39;

    private Theme() {
    }

    public static Font font(int style, float size) {
        return new Font("Segoe UI", style, Math.round(size));
    }

    /** FlatLaf extra defaults; must run before the look and feel is installed. */
    public static java.util.Map<String, String> flatLafDefaults() {
        // The window title bar reads its colours when the look and feel is installed.
        return java.util.Map.of("@accentColor", "#1E6FD9",
                "TitlePane.unifiedBackground", "false",
                "TitlePane.background", "#0A2540",
                "TitlePane.inactiveBackground", "#153E6B",
                "TitlePane.foreground", "#FFFFFF",
                "TitlePane.inactiveForeground", "#C3D9F0",
                "TitlePane.embeddedForeground", "#FFFFFF",
                "TitlePane.buttonHoverBackground", "#1F4E85",
                "TitlePane.buttonPressedBackground", "#153E6B");
    }

    public static void installDefaults() {
        UIManager.put("defaultFont", font(Font.PLAIN, 12));
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", new Color(0x8C, 0xC8, 0xF5, 0xB0));
        UIManager.put("Component.focusedBorderColor", PRIMARY);
        UIManager.put("Component.borderColor", FIELD_BORDER);
        UIManager.put("Button.borderColor", FIELD_BORDER);
        UIManager.put("Button.default.background", PRIMARY);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("List.selectionBackground", SELECTION);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SELECTION);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.thumb", new Color(0xB9, 0xD2, 0xEC));
        UIManager.put("ScrollBar.hoverThumbColor", new Color(0x8C, 0xB6, 0xE2));
        UIManager.put("Panel.background", CARD);
        // Tables sit inside rounded cards; the card draws the outline, so scroll panes have none.
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Viewport.background", CARD);
        UIManager.put("ScrollPane.background", CARD);
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.alternateRowColor", ROW_ALT);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("TextField.background", FIELD);
        UIManager.put("PasswordField.background", FIELD);
        UIManager.put("FormattedTextField.background", FIELD);
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("Spinner.background", FIELD);
        UIManager.put("Button.background", FIELD);
        // placeholders and disabled text readable, not washed-out grey
        Color placeholder = new Color(0x5F, 0x72, 0x8C);
        UIManager.put("TextField.placeholderForeground", placeholder);
        UIManager.put("PasswordField.placeholderForeground", placeholder);
        UIManager.put("FormattedTextField.placeholderForeground", placeholder);
        UIManager.put("ComboBox.placeholderForeground", placeholder);
        UIManager.put("TextField.inactiveForeground", new Color(0x4C, 0x5E, 0x78));
        UIManager.put("Label.disabledForeground", new Color(0x5F, 0x72, 0x8C));
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", TABLE_HEADER_TEXT);
        UIManager.put("ToolTip.background", NAVY);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        // Window title bar in the brand navy (FlatLaf window decorations on Windows).
        UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("TitlePane.background", NAVY);
        UIManager.put("TitlePane.inactiveBackground", NAVY_2);
        UIManager.put("TitlePane.foreground", Color.WHITE);
        UIManager.put("TitlePane.inactiveForeground", NAV_TEXT);
        UIManager.put("TitlePane.buttonHoverBackground", NAVY_3);
        UIManager.put("TitlePane.closeHoverBackground", DANGER);
        UIManager.put("TitlePane.embeddedForeground", Color.WHITE);
    }
}
