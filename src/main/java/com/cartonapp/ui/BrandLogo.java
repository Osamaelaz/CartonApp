package com.cartonapp.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Brand mark (carton box on a navy-to-blue tile) with the factory name and a short tagline. */
public final class BrandLogo extends JPanel {
    public static final String NAME = "الليثي باك";
    public static final String TAGLINE = "مصنع الكرتون المضلّع";

    /**
     * @param size  mark size in pixels
     * @param dark  true on a navy background (white name, baby-blue tagline)
     */
    public BrandLogo(int size, boolean dark) {
        super(new FlowLayout(FlowLayout.RIGHT, Math.max(8, size / 4), 0));
        setOpaque(false);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(new JLabel(mark(size)));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 0));
        text.setOpaque(false);
        JLabel name = new JLabel(NAME, SwingConstants.RIGHT);
        name.setFont(Theme.font(Font.BOLD, Math.max(14, size * 0.42f)));
        name.setForeground(dark ? Color.WHITE : Theme.NAVY);
        JLabel tagline = new JLabel(TAGLINE, SwingConstants.RIGHT);
        tagline.setFont(Theme.font(Font.PLAIN, Math.max(10, size * 0.24f)));
        tagline.setForeground(dark ? Theme.BABY : Theme.MUTED_TEXT);
        text.add(name);
        text.add(tagline);
        add(text);
    }

    public static Icon mark(int size) {
        return new FlatSVGIcon("icons/logo.svg", size, size, BrandLogo.class.getClassLoader());
    }

    /** Window/taskbar icons in several sizes, drawn from the same logo. */
    public static List<Image> windowIcons() {
        return FlatSVGUtils.createWindowIconImages("/icons/logo.svg");
    }
}
