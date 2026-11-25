package com.cartonapp.service;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Fonts for the sales-invoice document. The Excel template uses Calibri, which has no Arabic
 * glyphs; Excel substitutes the workbook theme's Arabic font, Arial. The same Arial files are used
 * for on-screen preview, printing (Java2D) and PDF embedding so all outputs measure text identically.
 */
public final class InvoiceFonts {
    private static final String[] REGULAR = {"arial.ttf", "tahoma.ttf", "segoeui.ttf"};
    private static final String[] BOLD = {"arialbd.ttf", "tahomabd.ttf", "segoeuib.ttf"};
    private static Font regular;
    private static Font bold;

    private InvoiceFonts() { }

    /** TrueType file used for PDF embedding, if one is installed. */
    public static Optional<Path> file(boolean boldFace) {
        String windows = System.getenv().getOrDefault("SystemRoot", "C:\\Windows");
        for (String name : boldFace ? BOLD : REGULAR) {
            Path path = Path.of(windows, "Fonts", name);
            if (Files.isRegularFile(path)) return Optional.of(path);
        }
        return Optional.empty();
    }

    /** Base AWT font (size 1) for measuring and Java2D drawing. */
    public static synchronized Font awt(boolean boldFace) {
        Font cached = boldFace ? bold : regular;
        if (cached != null) return cached;
        Font loaded = file(boldFace).map(path -> {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, path.toFile());
            } catch (FontFormatException | IOException exception) {
                return null;
            }
        }).orElse(new Font("Arial", boldFace ? Font.BOLD : Font.PLAIN, 1));
        if (boldFace) bold = loaded; else regular = loaded;
        return loaded;
    }
}
