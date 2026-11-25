package com.cartonapp.service;

import com.cartonapp.config.CompanyProfile;
import com.cartonapp.dao.SettingsDao;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Application settings: company details printed on invoices and reports, and the login password.
 * The password is stored only as a salted PBKDF2 hash. Until it is changed for the first time the
 * original password ({@value #DEFAULT_PASSWORD}) keeps working, so the accountant is never locked out.
 */
public final class SettingsService {
    public static final String DEFAULT_PASSWORD = "123";
    private static final String COMPANY_NAME = "company.name";
    private static final String COMPANY_ADDRESS = "company.address";
    private static final String COMPANY_PHONE = "company.phone";
    private static final String PASSWORD_HASH = "auth.password";
    private static final int ITERATIONS = 120_000;
    private static final int MIN_PASSWORD_LENGTH = 4;

    private final SettingsDao dao;

    public SettingsService(SettingsDao dao) {
        this.dao = dao;
    }

    /** Saved company details, or the invoice template's details when none were saved. */
    public CompanyProfile company() throws Exception {
        Map<String, String> values = dao.findAll();
        CompanyProfile fallback = CompanyProfile.loadDefault();
        return new CompanyProfile(values.getOrDefault(COMPANY_NAME, fallback.name()),
                values.getOrDefault(COMPANY_ADDRESS, fallback.address()),
                values.getOrDefault(COMPANY_PHONE, fallback.phone()));
    }

    public void saveCompany(CompanyProfile company) throws Exception {
        if (company.name().isBlank()) throw new IllegalArgumentException("اسم الشركة مطلوب");
        Map<String, String> values = new LinkedHashMap<>();
        values.put(COMPANY_NAME, company.name());
        values.put(COMPANY_ADDRESS, company.address());
        values.put(COMPANY_PHONE, company.phone());
        dao.save(values);
    }

    public boolean usesDefaultPassword() throws Exception {
        return dao.findAll().get(PASSWORD_HASH) == null;
    }

    public boolean authenticate(char[] password) throws Exception {
        if (password == null || password.length == 0) return false;
        String stored = dao.findAll().get(PASSWORD_HASH);
        if (stored == null) return MessageDigest.isEqual(new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                DEFAULT_PASSWORD.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return verify(password, stored);
    }

    /**
     * Changes the password after checking the current one. Returns Arabic problems; an empty list
     * means the new password was saved.
     */
    public List<String> changePassword(char[] current, char[] next, char[] confirm) throws Exception {
        List<String> problems = new ArrayList<>();
        if (current == null || current.length == 0) problems.add("أدخل كلمة المرور الحالية.");
        else if (!authenticate(current)) problems.add("كلمة المرور الحالية غير صحيحة.");
        if (next == null || next.length < MIN_PASSWORD_LENGTH)
            problems.add("كلمة المرور الجديدة يجب ألا تقل عن " + MIN_PASSWORD_LENGTH + " أحرف.");
        else if (new String(next).isBlank()) problems.add("كلمة المرور الجديدة لا يمكن أن تكون مسافات فقط.");
        if (next != null && confirm != null && !java.util.Arrays.equals(next, confirm))
            problems.add("تأكيد كلمة المرور لا يطابق كلمة المرور الجديدة.");
        if (next != null && current != null && next.length > 0 && java.util.Arrays.equals(next, current))
            problems.add("كلمة المرور الجديدة يجب أن تختلف عن الحالية.");
        if (!problems.isEmpty()) return problems;
        dao.save(Map.of(PASSWORD_HASH, hash(next)));
        return problems;
    }

    /** Short database description for the settings screen, e.g. "PostgreSQL 18.1". */
    public String databaseVersion() throws Exception {
        return "PostgreSQL " + dao.serverVersion();
    }

    private static String hash(char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2-sha256$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derived);
    }

    private static boolean verify(char[] password, String stored) throws GeneralSecurityException {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        return MessageDigest.isEqual(expected, derive(password, salt, Integer.parseInt(parts[1])));
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
