package com.cartonapp.model;

import java.math.BigDecimal;

/** Product row used by the catalogue and customer-product views. */
public record ProductSummary(long id, String code, String name, String customerCode,
        String customerName, String customerIndustry, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm,
        BigDecimal currentPrice, String boxType, int layerCount, int printColors, String specifications) {
    /** Paper specifications that can be ticked for a product. */
    public static final java.util.List<String> SPECIFICATIONS = java.util.List.of("كرافت", "فلوت", "تيست");

    public ProductSummary {
        specifications = specifications == null ? "" : specifications;
    }

    public ProductSummary(long id, String code, String name, String customerCode, String customerName,
            String customerIndustry, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm,
            BigDecimal currentPrice, String boxType, int layerCount, int printColors) {
        this(id, code, name, customerCode, customerName, customerIndustry, lengthCm, widthCm, heightCm, currentPrice,
                boxType, layerCount, printColors, "");
    }

    /** Ticked specifications, in the standard order. */
    public java.util.List<String> specificationList() {
        return java.util.List.copyOf(specificationCounts().keySet());
    }

    /**
     * Ticked specifications with their number (stored as "كرافت:2,فلوت:2"); entries saved before
     * numbers existed ("كرافت") count as 1.
     */
    public java.util.Map<String, Integer> specificationCounts() {
        java.util.Map<String, Integer> found = new java.util.HashMap<>();
        for (String part : specifications.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) continue;
            String[] pieces = item.split(":");
            int count = 1;
            if (pieces.length > 1) {
                try {
                    count = Math.max(1, Integer.parseInt(pieces[1].trim()));
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
            }
            found.put(pieces[0].trim(), count);
        }
        java.util.Map<String, Integer> ordered = new java.util.LinkedHashMap<>();
        for (String name : SPECIFICATIONS) if (found.containsKey(name)) ordered.put(name, found.get(name));
        return ordered;
    }

    /** e.g. "2 كرافت في 2 فلوت". */
    public String specificationText() {
        return String.join(" في ", specificationCounts().entrySet().stream()
                .map(e -> e.getValue() + " " + e.getKey()).toList());
    }

    public String dimensions() {
        return lengthCm.stripTrailingZeros().toPlainString() + " × "
                + widthCm.stripTrailingZeros().toPlainString() + " × "
                + heightCm.stripTrailingZeros().toPlainString() + " سم";
    }
}
