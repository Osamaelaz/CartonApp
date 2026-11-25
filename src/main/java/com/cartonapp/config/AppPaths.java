package com.cartonapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the installed application keeps its files. The program folder (Program Files) is read-only
 * for normal users, so settings and exports live in the user's own folders:
 * <ul>
 * <li>settings: {@code %APPDATA%\LeithiPack\} ({@code db.properties}, {@code company.properties})</li>
 * <li>exports: {@code Documents\الليثي باك\التصدير\}</li>
 * </ul>
 * For development, a {@code db.properties} next to where the program is started is still used when
 * the settings folder has none, and the system properties {@code cartonapp.home} and
 * {@code cartonapp.exports} override the folders (used by automated tests).
 */
public final class AppPaths {
    private AppPaths() { }

    /** Folder with db.properties and company.properties. */
    public static Path settingsDir() {
        String override = System.getProperty("cartonapp.home");
        if (override != null && !override.isBlank()) return Path.of(override);
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank() ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("LeithiPack");
    }

    /**
     * The settings file to read: the one in the settings folder, otherwise one next to the working
     * folder (development). The settings-folder path is returned when neither exists, so messages
     * can tell the user exactly where to put the file.
     */
    public static Path settingsFile(String name) {
        Path installed = settingsDir().resolve(name);
        if (Files.isRegularFile(installed)) return installed;
        Path local = Path.of(name);
        return Files.isRegularFile(local) ? local : installed;
    }

    /** Folder for exported PDF and Excel files; created when needed. */
    public static Path exportsDir() {
        String override = System.getProperty("cartonapp.exports");
        Path dir = override != null && !override.isBlank() ? Path.of(override)
                : Path.of(System.getProperty("user.home"), "Documents", "الليثي باك", "التصدير");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // the caller reports the failure when it writes the file
        }
        return dir;
    }
}
