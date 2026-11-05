package edu.evilshop.mechanics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WarrantyEngineTest {
    @Test
    void allOneHundredRollsProduceExactlyTheAdvertisedDistribution() {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            int value = i;
            WarrantyEngine.Prize prize = new WarrantyEngine(() -> value).spin();
            counts.merge(prize.code(), 1, Integer::sum);
            assertTrue(prize.simulated());
            assertEquals("Damage outside of shipping is not covered.", prize.exclusion());
        }
        assertEquals(Map.of("30_DAY", 80, "ONE_YEAR", 19, "LIFETIME", 1), counts);
    }

    @Test
    void boundariesReturnTheCorrectCoverage() {
        assertEquals(30, new WarrantyEngine(() -> 0).spin().coverageDays());
        assertEquals(30, new WarrantyEngine(() -> 79).spin().coverageDays());
        assertEquals(365, new WarrantyEngine(() -> 80).spin().coverageDays());
        assertEquals(365, new WarrantyEngine(() -> 98).spin().coverageDays());
        assertEquals(-1, new WarrantyEngine(() -> 99).spin().coverageDays());
    }

    @Test
    void invalidRollsCannotFallThroughToAPrize() {
        assertThrows(IllegalStateException.class, () -> new WarrantyEngine(() -> -1).spin());
        assertThrows(IllegalStateException.class, () -> new WarrantyEngine(() -> 100).spin());
        assertThrows(IllegalStateException.class, () -> new WarrantyEngine(() -> Integer.MAX_VALUE).spin());
        assertThrows(NullPointerException.class, () -> new WarrantyEngine(null));
    }

    @Test
    void quoteIsAnExactHufAmountAndOddsSumToOneHundred() {
        WarrantyEngine engine = new WarrantyEngine(() -> 0);
        assertEquals(new BigDecimal("4990.00"), engine.fee());
        assertEquals(100, engine.odds().stream().mapToInt(WarrantyEngine.OutcomeOdds::percentage).sum());
        assertThrows(UnsupportedOperationException.class, () -> engine.odds().clear());
    }

    @Test
    void productionRandomSourceOnlyReturnsSyntheticPrizes() {
        WarrantyEngine.Prize prize = new WarrantyEngine().spin();
        assertTrue(prize.simulated());
        assertTrue(Map.of("30_DAY", 30, "ONE_YEAR", 365, "LIFETIME", -1).containsKey(prize.code()));
    }
}
