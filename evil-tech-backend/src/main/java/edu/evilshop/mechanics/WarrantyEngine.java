package edu.evilshop.mechanics;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/** A synthetic warranty draw. This class never creates coverage or charges a payment method. */
public final class WarrantyEngine {
    private static final BigDecimal FEE = new BigDecimal("4990.00");
    private static final String EXCLUSION = "Damage outside of shipping is not covered.";
    private static final List<OutcomeOdds> ODDS = List.of(
            new OutcomeOdds("30_DAY", 80),
            new OutcomeOdds("ONE_YEAR", 19),
            new OutcomeOdds("LIFETIME", 1));

    private final IntSupplier roll;

    public WarrantyEngine() {
        SecureRandom random = new SecureRandom();
        this.roll = () -> random.nextInt(100);
    }

    /** Supplies a value in the inclusive range 0 through 99, primarily for deterministic tests. */
    public WarrantyEngine(IntSupplier roll) {
        this.roll = Objects.requireNonNull(roll, "roll must not be null");
    }

    public record Prize(String code, String label, int coverageDays, String exclusion,
                        boolean simulated) { }

    public record OutcomeOdds(String code, int percentage) { }

    public Prize spin() {
        int value = roll.getAsInt();
        if (value < 0 || value >= 100) {
            throw new IllegalStateException("Warranty roll must be between 0 and 99 inclusive");
        }
        if (value < 80) {
            return new Prize("30_DAY", "30 day warranty", 30, EXCLUSION, true);
        }
        if (value < 99) {
            return new Prize("ONE_YEAR", "1 year warranty", 365, EXCLUSION, true);
        }
        return new Prize("LIFETIME", "Lifetime coverage*", -1, EXCLUSION, true);
    }

    /** Flat simulated fee, denominated in HUF. */
    public BigDecimal fee() {
        return FEE;
    }

    public List<OutcomeOdds> odds() {
        return ODDS;
    }
}
