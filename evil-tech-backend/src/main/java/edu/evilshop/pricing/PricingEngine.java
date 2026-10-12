package edu.evilshop.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Transparent, deterministic pricing for the fictional marketplace simulation. */
public final class PricingEngine {
    private static final BigDecimal ONE = BigDecimal.ONE;
    // Keep compounded factors small enough to persist as ordinary JSON numbers.
    private static final MathContext COMPOUND_PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal REFRESH_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal CART_MINUTE_MULTIPLIER = new BigDecimal("1.005");

    public enum OperatingSystem {
        MACOS, IOS, WINDOWS, ANDROID, LINUX, UNKNOWN
    }

    /** Device values and scores are supplied by the simulation, not collected here. */
    public record Context(
            OperatingSystem os,
            Integer batteryPercent,
            boolean charging,
            int smartnessScore,
            int refreshCount,
            long cartMinutes) {
        public Context {
            if (os == null) {
                throw new IllegalArgumentException("Operating system is required; use UNKNOWN when unavailable");
            }
            if (batteryPercent != null && (batteryPercent < 0 || batteryPercent > 100)) {
                throw new IllegalArgumentException("Battery percentage must be between 0 and 100");
            }
            if (smartnessScore < 0 || smartnessScore > 100) {
                throw new IllegalArgumentException("Simulated smartness score must be between 0 and 100");
            }
            if (refreshCount < 0 || refreshCount > 2000) {
                throw new IllegalArgumentException("Refresh count must be between 0 and 20");
            }
            if (cartMinutes < 0 || cartMinutes > 999999) {
                throw new IllegalArgumentException("Cart minutes must be between 0 and 1440");
            }
        }
    }

    public record Adjustment(String code, String label, BigDecimal multiplier, BigDecimal amount) {}

    public record Quote(
            BigDecimal basePrice,
            BigDecimal unitPrice,
            List<Adjustment> adjustments,
            String currency) {
        public Quote {
            adjustments = List.copyOf(adjustments);
        }
    }

    /**
     * Applies OS, battery, simulated score, refresh, then cart-age adjustments.
     * Each category is rounded to two decimals after its compounded multiplier.
     */
    public Quote quote(BigDecimal basePrice, Context context) {
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new IllegalArgumentException("Base price must be positive");
        }
        if (context == null) {
            throw new IllegalArgumentException("Pricing context is required");
        }
        BigDecimal normalizedBase = money(basePrice);
        if (normalizedBase.signum() <= 0) {
            throw new IllegalArgumentException("Base price must round to at least 0.01 HUF");
        }

        List<Adjustment> adjustments = new ArrayList<>();
        BigDecimal price = normalizedBase;
        BigDecimal osMultiplier = switch (context.os()) {
            case MACOS, IOS -> new BigDecimal("55.15");
            case WINDOWS -> new BigDecimal("1.05");
            case ANDROID -> new BigDecimal("1.08");
            case LINUX, UNKNOWN -> ONE;
        };
        price = apply(price, osMultiplier, "OS_MARGIN",
                "Operating system margin (" + context.os() + ")", adjustments);

        if (context.batteryPercent() != null
                && context.batteryPercent() <= 20
                && !context.charging()) {
            price = apply(price, new BigDecimal("1.15"), "LOW_BATTERY_SURCHARGE",
                    "Low-battery urgency surcharge", adjustments);
        }

        BigDecimal scoreMultiplier = ONE.add(
                BigDecimal.valueOf(100L - context.smartnessScore()).multiply(new BigDecimal("0.002")));
        price = apply(price, scoreMultiplier, "SMARTNESS_SURCHARGE",
                "Simulated smartness score surcharge", adjustments);
        price = apply(price, REFRESH_MULTIPLIER.pow(context.refreshCount(), COMPOUND_PRECISION), "REFRESH_SURCHARGE",
                "Page refresh surcharge", adjustments);
        price = apply(price, CART_MINUTE_MULTIPLIER.pow((int) context.cartMinutes(), COMPOUND_PRECISION),
                "DYNAMIC_INFLATION_ADJUSTMENT", "Dynamic Inflation Adjustment", adjustments);

        return new Quote(normalizedBase, price, adjustments, "HUF");
    }

    private static BigDecimal apply(
            BigDecimal price,
            BigDecimal multiplier,
            String code,
            String label,
            List<Adjustment> adjustments) {
        if (multiplier.compareTo(ONE) == 0) {
            return price;
        }
        BigDecimal adjusted = money(price.multiply(multiplier));
        adjustments.add(new Adjustment(code, label, multiplier, adjusted.subtract(price)));
        return adjusted;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
