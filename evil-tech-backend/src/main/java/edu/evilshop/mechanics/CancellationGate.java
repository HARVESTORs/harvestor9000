package edu.evilshop.mechanics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demonstrates an absurd cancellation obstacle using a supplied text transcript only.
 * It performs no microphone capture, speaker recognition, or biometric comparison.
 * The direct cancellation route is available independently of this demonstration.
 */
public final class CancellationGate {
    public static final int MAX_TRANSCRIPT_CHARACTERS = 20_000;
    public static final String BYPASS_PATH = "/api/subscription/cancel-direct";
    private static final int REQUIRED_WORD_COUNT = 500;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private final String disclaimer;
    private final List<String> expectedWords;

    public CancellationGate() {
        try (InputStream stream = CancellationGate.class.getResourceAsStream("/cancellation-disclaimer.txt")) {
            if (stream == null) {
                throw new IllegalStateException("Cancellation disclaimer resource is missing");
            }
            disclaimer = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read cancellation disclaimer", error);
        }
        expectedWords = words(disclaimer);
        if (expectedWords.size() != REQUIRED_WORD_COUNT) {
            throw new IllegalStateException("Cancellation disclaimer must contain exactly 500 words");
        }
    }

    public record Challenge(String disclaimer, int wordCount, String verificationMode,
                            String bypassPath, boolean simulated) { }

    /** matchedWords counts words matching at the same position, not a word-count-only score. */
    public record Verification(boolean passed, int expectedWords, int matchedWords, String message) { }

    public Challenge challenge() {
        return new Challenge(disclaimer, expectedWords.size(), "SIMULATED_TRANSCRIPT", BYPASS_PATH, true);
    }

    public Verification verify(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return rejected("Provide a text transcript, or use direct cancellation.");
        }
        if (transcript.length() > MAX_TRANSCRIPT_CHARACTERS) {
            return rejected("Transcript must be at most 20000 characters. Direct cancellation is available.");
        }

        List<String> actualWords = words(transcript);
        int matched = 0;
        for (int i = 0; i < Math.min(expectedWords.size(), actualWords.size()); i++) {
            if (expectedWords.get(i).equals(actualWords.get(i))) {
                matched++;
            }
        }
        boolean passed = expectedWords.equals(actualWords);
        String message = passed
                ? "Simulated transcript accepted. No audio or biometric information was collected."
                : "Transcript must match all 500 words in order. Direct cancellation is always available.";
        return new Verification(passed, expectedWords.size(), matched, message);
    }

    private Verification rejected(String message) {
        return new Verification(false, expectedWords.size(), 0, message);
    }

    private static List<String> words(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return List.copyOf(result);
    }
}
