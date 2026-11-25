package com.cartonapp.service;

/** Login check against the password saved in the settings table (see {@link SettingsService}). */
public final class DatabaseAuthenticationService implements AuthenticationService {
    private final SettingsService settings;

    public DatabaseAuthenticationService(SettingsService settings) {
        this.settings = settings;
    }

    /** Short readable cause (never contains the password): wrong password, unknown host, no internet … */
    static String reason(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String text = String.valueOf(cause.getMessage()).replaceAll("\\s+", " ").trim();
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("password authentication failed") || lower.contains("authentication failed"))
            return "اسم المستخدم أو كلمة مرور قاعدة البيانات غير صحيحة (راجع db.properties)";
        if (cause instanceof java.net.UnknownHostException || lower.contains("unknownhost"))
            return "عنوان الخادم غير صحيح أو لا يوجد إنترنت";
        if (lower.contains("timed out") || lower.contains("timeout"))
            return "انتهت مهلة الاتصال (تحقق من الإنترنت وأعد المحاولة)";
        if (lower.contains("does not exist") && lower.contains("database"))
            return "قاعدة البيانات المذكورة في الرابط غير موجودة";
        if (lower.contains("ssl"))
            return "مشكلة في اتصال SSL (تأكد أن الرابط فيه sslmode=require)";
        return text.length() > 160 ? text.substring(0, 160) + "…" : text;
    }

    @Override
    public boolean authenticate(char[] password) {
        try {
            return settings.authenticate(password);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new IllegalStateException("تعذر الاتصال بقاعدة البيانات: " + reason(exception), exception);
        }
    }
}
