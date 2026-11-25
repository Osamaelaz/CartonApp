package com.cartonapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Company identity printed at the top of every invoice. Defaults come from the official
 * sales-invoice Excel template; an optional UTF-8 {@code company.properties} file next to
 * {@code db.properties} can override them.
 */
public record CompanyProfile(String name, String address, String phone) {
    public static final CompanyProfile TEMPLATE_DEFAULT =
            new CompanyProfile("الليثي باك", "ميت غمر - الدقهلية", "01014484407");

    public CompanyProfile {
        name = name == null ? "" : name.trim();
        address = address == null ? "" : address.trim();
        phone = phone == null ? "" : phone.trim();
    }

    /** Address and phone in the template's "address | phone" form. */
    public String contactLine() {
        if (address.isEmpty()) return phone;
        if (phone.isEmpty()) return address;
        return address + " | " + phone;
    }

    public static CompanyProfile loadDefault() {
        Path file = AppPaths.settingsFile("company.properties");
        if (!Files.isRegularFile(file)) return TEMPLATE_DEFAULT;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            return TEMPLATE_DEFAULT;
        }
        return new CompanyProfile(
                properties.getProperty("company.name", TEMPLATE_DEFAULT.name()),
                properties.getProperty("company.address", TEMPLATE_DEFAULT.address()),
                properties.getProperty("company.phone", TEMPLATE_DEFAULT.phone()));
    }
}
