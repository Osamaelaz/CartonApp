package com.cartonapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/** Immutable local PostgreSQL settings loaded from the untracked db.properties file. */
public record DatabaseConfig(String url, String user, String password) {
    public DatabaseConfig {
        url = required(url, "db.url");
        user = required(user, "db.user");
        password = Objects.requireNonNull(password, "db.password must be present");
    }

    public static DatabaseConfig loadDefault() throws IOException {
        return load(AppPaths.settingsFile("db.properties"));
    }

    public static DatabaseConfig load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Database settings file not found: " + file.toAbsolutePath());
        }
        Properties properties = new Properties();
        // Read as UTF-8 and drop the invisible byte-order mark that Notepad adds ("UTF-8 with BOM"),
        // which would otherwise turn the first key into "﻿db.url".
        String text = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        properties.load(new java.io.StringReader(text));
        return of(properties.getProperty("db.url"), properties.getProperty("db.user"),
                properties.getProperty("db.password"));
    }

    /**
     * Builds the settings from the file values. {@code db.url} may be a JDBC URL
     * ({@code jdbc:postgresql://host/db}) or the connection link hosted PostgreSQL services such as
     * Neon give out ({@code postgresql://user:password@host/db?sslmode=require}); in the second form
     * the user and password inside the link are used, even if {@code db.user} / {@code db.password}
     * are also present.
     */
    public static DatabaseConfig of(String url, String user, String password) {
        String text = url == null ? "" : url.trim();
        if (text.startsWith("postgresql://") || text.startsWith("postgres://")) {
            try {
                java.net.URI uri = new java.net.URI(text);
                String info = uri.getRawUserInfo();
                if (info != null) {
                    int colon = info.indexOf(':');
                    String linkUser = decode(colon < 0 ? info : info.substring(0, colon));
                    String linkPassword = colon < 0 ? null : decode(info.substring(colon + 1));
                    // The link is explicit: its user and password win over any separate db.user /
                    // db.password lines (e.g. leftovers from a local database setup).
                    if (!linkUser.isBlank()) user = linkUser;
                    if (linkPassword != null) password = linkPassword;
                }
                StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
                if (uri.getPort() > 0) jdbc.append(':').append(uri.getPort());
                jdbc.append(uri.getRawPath() == null ? "" : uri.getRawPath());
                String query = uri.getRawQuery();
                if (query != null) {
                    // channel_binding is a libpq-only option; the JDBC driver does not need it
                    String kept = java.util.Arrays.stream(query.split("&"))
                            .filter(part -> !part.startsWith("channel_binding=")).collect(java.util.stream.Collectors.joining("&"));
                    if (!kept.isEmpty()) jdbc.append('?').append(kept);
                }
                text = jdbc.toString();
            } catch (java.net.URISyntaxException exception) {
                throw new IllegalArgumentException("db.url is not a valid connection link: " + exception.getMessage());
            }
        }
        return new DatabaseConfig(text, user, password);
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must be configured in db.properties");
        }
        return value.trim();
    }
}
