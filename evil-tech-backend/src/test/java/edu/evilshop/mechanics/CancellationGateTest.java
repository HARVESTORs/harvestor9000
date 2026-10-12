package edu.evilshop.mechanics;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CancellationGateTest {
    private final CancellationGate gate = new CancellationGate();

    @Test
    void challengeHasExactlyFiveHundredWordsAndAnIndependentBypass() {
        CancellationGate.Challenge challenge = gate.challenge();
        assertEquals(500, challenge.wordCount());
        assertEquals(500, challenge.disclaimer().split("\\s+").length);
        assertEquals("SIMULATED_TRANSCRIPT", challenge.verificationMode());
        assertEquals("/api/subscription/cancel-direct", challenge.bypassPath());
        assertTrue(challenge.simulated());
    }

    @Test
    void exactDisclaimerPasses() {
        CancellationGate.Verification result = gate.verify(gate.challenge().disclaimer());
        assertTrue(result.passed());
        assertEquals(500, result.expectedWords());
        assertEquals(500, result.matchedWords());
    }

    @Test
    void capitalizationPunctuationAndWhitespaceDoNotMatter() {
        String modified = gate.challenge().disclaimer()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[.,]", "")
                .replaceAll("\\s+", " ;!?\n");
        assertTrue(gate.verify(modified).passed());
        assertEquals(500, gate.verify(modified).matchedWords());
    }

    @Test
    void unrelatedFiveHundredWordTranscriptIsRejected() {
        CancellationGate.Verification result = gate.verify("banana ".repeat(500));
        assertFalse(result.passed());
        assertEquals(500, result.expectedWords());
        assertEquals(0, result.matchedWords());
    }

    @Test
    void correctWordsInTheWrongOrderAreRejected() {
        String reordered = gate.challenge().disclaimer().replaceFirst("Welcome to", "to Welcome");
        assertFalse(gate.verify(reordered).passed());
        assertEquals(498, gate.verify(reordered).matchedWords());
    }

    @Test
    void missingOrAdditionalWordsAreRejected() {
        String text = gate.challenge().disclaimer();
        assertFalse(gate.verify(text.replaceFirst("Welcome ", "")).passed());
        CancellationGate.Verification extra = gate.verify(text + " surprise");
        assertFalse(extra.passed());
        assertEquals(500, extra.matchedWords());
    }

    @Test
    void nullBlankPunctuationOnlyAndOversizedInputAreRejected() {
        assertFalse(gate.verify(null).passed());
        assertFalse(gate.verify("").passed());
        assertFalse(gate.verify(" \n\t ").passed());
        assertFalse(gate.verify("... !?").passed());
        CancellationGate.Verification oversized = gate.verify("x".repeat(20_001));
        assertFalse(oversized.passed());
        assertEquals(0, oversized.matchedWords());
        assertTrue(oversized.message().contains("20000"));
    }

    @Test
    void maximumLengthInputIsAcceptedForVerification() {
        String text = gate.challenge().disclaimer();
        String padded = text + " ".repeat(20_000 - text.length());
        assertEquals(20_000, padded.length());
        assertTrue(gate.verify(padded).passed());
    }
}
