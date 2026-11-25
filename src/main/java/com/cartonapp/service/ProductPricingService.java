package com.cartonapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculates the derived carton price from sheet dimensions and the current base cost. */
public final class ProductPricingService {
    private static final BigDecimal AVERAGE_LAYER_PRICE = new BigDecimal("33.75");
    private static final BigDecimal MANUFACTURING_COST_PER_TON = new BigDecimal("6500");
    private static final BigDecimal AVERAGE_GRAMMAGE = new BigDecimal("151.6666666667");
    private static final BigDecimal GRAMMAGE_ADD = new BigDecimal("7");
    private static final BigDecimal CORRUGATION_FACTOR = new BigDecimal("3.3");

    private ProductPricingService() { }

    /** The pricing constants, as label and value, for the read-only settings screen. */
    public static java.util.List<String[]> constants() {
        return java.util.List.of(
                new String[] {"متوسط سعر الطبقات", AVERAGE_LAYER_PRICE.toPlainString() + " ج.م"},
                new String[] {"المصنعية للطن", MANUFACTURING_COST_PER_TON.toPlainString() + " ج.م"},
                new String[] {"متوسط الجرام", AVERAGE_GRAMMAGE.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " + " + GRAMMAGE_ADD.toPlainString() + " جم"},
                new String[] {"معامل التعريج", CORRUGATION_FACTOR.toPlainString()});
    }

    /** Intermediate values of the pricing formula for one carton, for the product page. */
    public record Breakdown(BigDecimal sheetLength, BigDecimal sheetWidth, BigDecimal sheetArea,
            BigDecimal pricePerTon, BigDecimal grammage, BigDecimal corrugationFactor, BigDecimal price) { }

    public static Breakdown breakdown(BigDecimal length, BigDecimal width, BigDecimal height) {
        BigDecimal sheetLength = length.add(width).multiply(new BigDecimal("2"));
        BigDecimal sheetWidth = width.add(height);
        BigDecimal area = sheetLength.multiply(sheetWidth).divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
        return new Breakdown(sheetLength, sheetWidth, area, AVERAGE_LAYER_PRICE.add(MANUFACTURING_COST_PER_TON),
                AVERAGE_GRAMMAGE.add(GRAMMAGE_ADD).setScale(2, RoundingMode.HALF_UP), CORRUGATION_FACTOR,
                calculate(length, width, height));
    }

    public static BigDecimal calculate(BigDecimal length, BigDecimal width, BigDecimal height) {
        BigDecimal sheetLength = length.add(width).multiply(new BigDecimal("2"));
        BigDecimal sheetWidth = width.add(height);
        BigDecimal sheetAreaSquareMeters = sheetLength.multiply(sheetWidth)
                .divide(new BigDecimal("10000"), 10, RoundingMode.HALF_UP);
        BigDecimal pricePerTon = AVERAGE_LAYER_PRICE.add(MANUFACTURING_COST_PER_TON);
        BigDecimal grammage = AVERAGE_GRAMMAGE.add(GRAMMAGE_ADD);
        return sheetAreaSquareMeters.multiply(pricePerTon)
                .multiply(CORRUGATION_FACTOR)
                .multiply(grammage)
                .divide(new BigDecimal("1000000"), 2, RoundingMode.HALF_UP);
    }
}
