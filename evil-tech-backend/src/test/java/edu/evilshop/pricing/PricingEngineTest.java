package edu.evilshop.pricing;

import static edu.evilshop.pricing.PricingEngine.OperatingSystem.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingEngineTest {
    private final PricingEngine pricing = new PricingEngine();

    @Test
    void composesAdjustmentsInOrderAndMakesEverySurchargeVisible() {
        PricingEngine.Quote quote = pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(MACOS, 20, false, 50, 1, 1));

        assertEquals(new BigDecimal("1000.00"), quote.basePrice());
        assertEquals(new BigDecimal("1754.43"), quote.unitPrice());
        assertEquals("HUF", quote.currency());
        assertEquals(List.of("OS_MARGIN", "LOW_BATTERY_SURCHARGE", "SMARTNESS_SURCHARGE",
                        "REFRESH_SURCHARGE", "DYNAMIC_INFLATION_ADJUSTMENT"),
                quote.adjustments().stream().map(PricingEngine.Adjustment::code).toList());
        assertEquals(List.of(new BigDecimal("150.00"), new BigDecimal("172.50"),
                        new BigDecimal("132.25"), new BigDecimal("290.95"), new BigDecimal("8.73")),
                quote.adjustments().stream().map(PricingEngine.Adjustment::amount).toList());
        assertEquals(quote.unitPrice(), quote.basePrice().add(quote.adjustments().stream()
                .map(PricingEngine.Adjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void cartInflationBeginsAtOneCompletedMinuteAndCompounds() {
        assertEquals(new BigDecimal("1000.00"), neutralQuote(0, 0).unitPrice());
        assertEquals(new BigDecimal("1005.00"), neutralQuote(0, 1).unitPrice());
        assertEquals(new BigDecimal("1010.03"), neutralQuote(0, 2).unitPrice());
        assertEquals(new BigDecimal("1440.00"), neutralQuote(2, 0).unitPrice());
        assertTrue(neutralQuote(0, 0).adjustments().isEmpty());
    }

    @Test
    void longLivedCartMultipliersStayCompactWithoutChangingRoundedMoney() {
        for (int minutes : new int[] {334, 1440}) {
            var quote = neutralQuote(20, minutes);
            BigDecimal expectedAfterRefresh = new BigDecimal("1000.00")
                    .multiply(new BigDecimal("1.20").pow(20))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedPrice = expectedAfterRefresh
                    .multiply(new BigDecimal("1.005").pow(minutes))
                    .setScale(2, RoundingMode.HALF_UP);

            assertEquals(expectedPrice, quote.unitPrice());
            assertTrue(quote.adjustments().stream()
                    .allMatch(adjustment -> adjustment.multiplier().precision() <= 34));
            assertEquals(quote.unitPrice(), quote.basePrice().add(quote.adjustments().stream()
                    .map(PricingEngine.Adjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
    }

    @Test
    void operatingSystemMarginsMatchTheSimulationRules() {
        assertOsPrice(MACOS, "1150.00");
        assertOsPrice(IOS, "1150.00");
        assertOsPrice(WINDOWS, "1050.00");
        assertOsPrice(ANDROID, "1080.00");
        assertOsPrice(LINUX, "1000.00");
        assertOsPrice(UNKNOWN, "1000.00");
    }

    @Test
    void batteryThresholdIsInclusiveButChargingAndMissingValuesAreExempt() {
        assertEquals(new BigDecimal("1150.00"), batteryQuote(0, false).unitPrice());
        assertEquals(new BigDecimal("1150.00"), batteryQuote(20, false).unitPrice());
        assertEquals(new BigDecimal("1000.00"), batteryQuote(21, false).unitPrice());
        assertEquals(new BigDecimal("1000.00"), batteryQuote(20, true).unitPrice());
        assertEquals(new BigDecimal("1000.00"), batteryQuote(null, false).unitPrice());
    }

    @Test
    void lowestSimulatedScoreHasTwentyPercentSurcharge() {
        var quote = pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(UNKNOWN, null, false, 0, 0, 0));
        assertEquals(new BigDecimal("1200.00"), quote.unitPrice());
        assertEquals("SMARTNESS_SURCHARGE", quote.adjustments().get(0).code());
    }

    @Test
    void roundsEachCategoryHalfUp() {
        var quote = pricing.quote(new BigDecimal("0.10"),
                new PricingEngine.Context(WINDOWS, 20, false, 100, 0, 0));
        assertEquals(new BigDecimal("0.01"), quote.adjustments().get(0).amount());
        assertEquals(new BigDecimal("0.02"), quote.adjustments().get(1).amount());
        assertEquals(new BigDecimal("0.13"), quote.unitPrice());
    }

    @Test
    void acceptsInclusiveContextBoundsAndRejectsAnythingOutside() {
        assertDoesNotThrow(() -> new PricingEngine.Context(UNKNOWN, 0, false, 0, 0, 0));
        assertDoesNotThrow(() -> pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(UNKNOWN, 100, true, 100, 20, 1440)));
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(null, null, false, 100, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, -1, false, 100, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, 101, false, 100, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, -1, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, 101, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, 100, -1, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, 100, 21, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, 100, 0, -1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PricingEngine.Context(UNKNOWN, null, false, 100, 0, 1441)));
    }

    @Test
    void rejectsMissingAndNonpositivePrices() {
        var context = new PricingEngine.Context(UNKNOWN, null, false, 100, 0, 0);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> pricing.quote(null, context)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> pricing.quote(BigDecimal.ZERO, context)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> pricing.quote(new BigDecimal("-1"), context)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> pricing.quote(new BigDecimal("0.001"), context)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> pricing.quote(BigDecimal.ONE, null)));
    }

    private PricingEngine.Quote neutralQuote(int refreshes, long minutes) {
        return pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(UNKNOWN, null, false, 100, refreshes, minutes));
    }

    private PricingEngine.Quote batteryQuote(Integer battery, boolean charging) {
        return pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(UNKNOWN, battery, charging, 100, 0, 0));
    }

    private void assertOsPrice(PricingEngine.OperatingSystem os, String expectedPrice) {
        assertEquals(new BigDecimal(expectedPrice), pricing.quote(new BigDecimal("1000"),
                new PricingEngine.Context(os, null, false, 100, 0, 0)).unitPrice());
    }
}
