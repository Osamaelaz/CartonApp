package com.cartonapp.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import javax.swing.Icon;

/** Loads the Make icon set from application resources. */
public final class Icons {
    private Icons() { }
    public static Icon icon(String name) { return icon(name, 18, null); }
    public static Icon icon(String name, int size) { return icon(name, size, null); }
    public static Icon icon(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size, Icons.class.getClassLoader());
        if (color != null) icon.setColorFilter(new FlatSVGIcon.ColorFilter(ignored -> color));
        return icon;
    }
}
