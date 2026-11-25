package com.cartonapp;

import com.cartonapp.config.DatabaseConfig;
import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.dao.SettingsDao;
import com.cartonapp.service.AuthenticationService;
import com.cartonapp.service.DatabaseAuthenticationService;
import com.cartonapp.service.SampleAuthenticationService;
import com.cartonapp.service.SettingsService;
import com.cartonapp.ui.LoginFrame;
import com.cartonapp.ui.Theme;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. */
public final class Main {
    private Main() {
    }

    /** Password from the settings table; without db.properties only the default password works. */
    private static AuthenticationService authentication() {
        try {
            return new DatabaseAuthenticationService(new SettingsService(
                    new SettingsDao(new DatabaseConnectionFactory(DatabaseConfig.loadDefault()))));
        } catch (Exception exception) {
            return new SampleAuthenticationService();
        }
    }

    /** Message shown when the database cannot be prepared (missing settings file or PostgreSQL not running). */
    private static void reportDatabaseProblem(Exception exception) {
        java.nio.file.Path folder = com.cartonapp.config.AppPaths.settingsDir();
        try {
            java.nio.file.Files.createDirectories(folder);
        } catch (java.io.IOException ignored) {
            // shown in the message below anyway
        }
        String nl = System.lineSeparator();
        boolean missing = exception instanceof java.io.IOException || exception instanceof IllegalArgumentException;
        String message = missing
                ? "ملف إعدادات قاعدة البيانات (db.properties) غير موجود أو غير صالح." + nl + nl
                        + "ضعه في هذا المجلد ثم أعد تشغيل البرنامج:" + nl + folder
                : "تعذر الاتصال بقاعدة البيانات." + nl + nl
                        + "تأكد أن PostgreSQL يعمل وأن بيانات الدخول في الملف صحيحة:" + nl
                        + com.cartonapp.config.AppPaths.settingsFile("db.properties");
        javax.swing.JOptionPane.showMessageDialog(null, message + nl + nl + "(" + exception.getMessage() + ")",
                "الليثي باك — قاعدة البيانات", javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(Theme.flatLafDefaults());
            FlatLightLaf.setup();
            Theme.installDefaults();
        });
        // Database work stays off the Swing thread: create missing tables, then open the login screen.
        Exception problem = null;
        try {
            DatabaseBootstrap.ensureReady();
        } catch (Exception exception) {
            problem = exception;
        }
        Exception failure = problem;
        SwingUtilities.invokeLater(() -> {
            if (failure != null)
                reportDatabaseProblem(failure);
            LoginFrame loginFrame = new LoginFrame(authentication());
            loginFrame.setVisible(true);
        });
    }
}
