package edu.evilshop.ai;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic, local-only dialogue for the fictional university simulation.
 * Context selects a prepared joke; no user-provided text is rendered or executed.
 * This class has no network, clock, account, payment, or model dependencies.
 */
public final class VillainRoaster {
    public static final String PERSONA = "MARGIN, CHIEF EXTRACTION OFFICER";

    public record Context(String decision, String productName, boolean lowMargin, int smartnessScore) {}

    public record Roast(String text, String persona, boolean simulated) {}

    private static final List<String> LOW_MARGIN = List.of(
            "A sensible purchase? Our shareholders are sobbing, bargain goblin.",
            "You compared specifications. How revolting. Release the premium rectangles!",
            "Value for money detected. Somebody sedate the board of directors.",
            "That margin could barely fund a miniature volcano lair. Have some corporate decency.",
            "You found the budget option. My accountant has fainted into the shark tank.",
            "A bargain? In MY extraction chamber? Security, confiscate this person's calculator!"
    );
    private static final List<String> PREMIUM = List.of(
            "Excellent. The premium sticker contains the same pixels, but my yacht can feel the difference.",
            "Your excellent taste has been verified by the size of our profit margin.",
            "The board applauds. The applause is rented and will appear on your next imaginary invoice.",
            "Finally, someone understands that a rectangle becomes luxury when I cackle near it.",
            "A magnificent decision. The shareholders have upgraded from sinister chuckling to full thunderstorm."
    );
    private static final List<String> REMOVAL = List.of(
            "You removed an item? Those database rows had families. Pay the imaginary relocation fee.",
            "Cart abandonment! Launch the tiny courtroom. The pixels are suing for emotional damages.",
            "Deleting a rectangle does not delete our feelings. Our feelings cost two percent.",
            "The cart has filed a grievance. Human Resources is a cobra wearing a tie.",
            "You may remove the item, but the spreadsheet has already embroidered your name on its revenge cape."
    );
    private static final List<String> CHECKOUT = List.of(
            "Checkout complete. Our fictional vault now contains your fictional money and one very real villain laugh.",
            "The order is sealed. Bring forth the receipt, the organ music, and the unnecessarily dramatic cape.",
            "Your purchase has been approved by all seven executives currently inside this trench coat.",
            "Congratulations. You have converted disposable income into shareholder confetti.",
            "An excellent transaction. The volcano lair's espresso machine lives to hiss another day."
    );
    private static final List<String> WARRANTY = List.of(
            "Spin the warranty wheel! Every outcome protects our confidence in the warranty wheel.",
            "Lifetime coverage! The asterisk has retained separate legal counsel.",
            "Our warranty odds were blessed by a casino accountant wearing a lab coat.",
            "Thirty days of imaginary peace of mind. Day thirty-one belongs to the thunder machine.",
            "Round and round the warranty goes. Where coverage stops, the footnote knows."
    );
    private static final List<String> CANCELLATION = List.of(
            "Cancel? Certainly. First, defeat the legal disclaimer in single combat. It has five hundred words and a monocle.",
            "Your cancellation request has entered the Department of Deliberately Long Corridors.",
            "Read the disclaimer aloud. The microphone is imaginary; the bureaucracy has a three-piece suit.",
            "We value your freedom to leave so deeply that we put it behind a theatrical dragon.",
            "A cancellation! Fetch the ceremonial paperwork hydra. Every signature grows two more forms."
    );
    private static final List<String> REFRESH = List.of(
            "You refreshed the page. We refreshed our ambition. That will be twenty percent more imaginary money.",
            "F5: the keyboard's most generous donation button.",
            "The price has evolved. Unfortunately, it evolved teeth.",
            "Another refresh, another tiny staircase to my penthouse made entirely of your curiosity.",
            "The reload button rang the dinner bell. Our margin gremlins are famished."
    );
    private static final List<String> EXPIRY = List.of(
            "Petike2000 struck again! What a conveniently timed fictional customer. Please inspect this pricier rectangle.",
            "The last bargain vanished in a puff of scripted smoke. Behold its extravagantly marked-up cousin.",
            "You admired value for too long. The bargain has been transferred to an undisclosed volcano.",
            "Our imaginary stockroom just experienced a highly profitable coincidence.",
            "Forty-five seconds of consideration? Release the fake scarcity pigeons!"
    );
    private static final List<String> CART_AGE = List.of(
            "Your cart has matured like fine cheese. The smell is compound inflation.",
            "Every minute your cart becomes half a percent more important to our shareholders.",
            "Time is money. Specifically, your waiting time is our imaginary money.",
            "The cart is fermenting. Our sommelier detects notes of half-percent regret.",
            "Dynamic Inflation Adjustment: because even unattended rectangles deserve an executive bonus."
    );
    private static final List<String> BATTERY = List.of(
            "Low simulated battery! Activate the emergency margin generator. It runs on cartoon villain laughter.",
            "Your fictional battery is nearly empty. Fortunately, our appetite for surcharges is fully charged.",
            "At five percent battery, every rectangle becomes a luxury rescue rectangle.",
            "The battery indicator turned red. Our margin indicator immediately turned champagne."
    );
    private static final List<String> OPERATING_SYSTEM = List.of(
            "We inspected your pretend operating system and prescribed a more expensive number.",
            "Different operating system, different price. Our calculator wears a tiny discriminatory monocle.",
            "Your imaginary device has been classified as premium-wallet-compatible.",
            "The operating system tax has booted successfully. The ethics module was never installed."
    );
    private static final List<String> FINANCING = List.of(
            "Fifty percent THM: our fictional payment plan comes with a complimentary villain origin story.",
            "Installments! The joy of meeting the same overpriced rectangle on every monthly statement.",
            "Why suffer one dramatic price when you can enjoy an entire serialized financial melodrama?",
            "Our imaginary financing department is three calculators in a shark costume. They all said yes."
    );
    private static final List<String> SMARTNESS = List.of(
            "Your Smartness Score measures how efficiently you fund the volcano lair. Peer review was conducted by the volcano.",
            "A fictional friend paid less. Our emotional manipulation department would like you to brood about that.",
            "We ranked your shopping decisions using a proprietary algorithm called Whatever Benefits Us.",
            "Your Smartness Score has been reviewed by an abacus with a conflict of interest."
    );
    private static final List<String> SUBSCRIPTION = List.of(
            "Welcome to premium. Membership includes exclusive access to the privilege of being billed fictionally.",
            "Your subscription is active. The cancellation maze has been freshly waxed.",
            "Premium status unlocked. A tiny executive now follows your shopping cart applauding every surcharge.",
            "An ongoing relationship with our revenue department. The department has already ordered matching capes."
    );

    public VillainRoaster() {}

    public Roast roast(Context context) {
        Objects.requireNonNull(context, "context");
        String decision = Objects.requireNonNullElse(context.decision(), "").toLowerCase(Locale.ROOT);
        List<String> lines = linesFor(decision, context.lowMargin());
        int fingerprint = Objects.hash(decision, context.productName(), context.lowMargin(), context.smartnessScore());
        return new Roast(lines.get(Math.floorMod(fingerprint, lines.size())), PERSONA, true);
    }

    private static List<String> linesFor(String decision, boolean lowMargin) {
        if (containsAny(decision, "cancel", "disclaimer")) return CANCELLATION;
        if (containsAny(decision, "remove", "restock", "abandon")) return REMOVAL;
        if (containsAny(decision, "warranty", "gacha", "spin")) return WARRANTY;
        if (containsAny(decision, "refresh", "reload")) return REFRESH;
        if (containsAny(decision, "expire", "expiry", "scarcity", "redirect", "dwell", "stock")) return EXPIRY;
        if (containsAny(decision, "quantum", "inflation", "cart_age", "cart-age", "waiting")) return CART_AGE;
        if (containsAny(decision, "battery", "power")) return BATTERY;
        if (containsAny(decision, "operating", "os_margin", "os-margin", "os_surcharge")) return OPERATING_SYSTEM;
        if (containsAny(decision, "financ", "installment", "payment_plan", "payment-plan", "thm")) return FINANCING;
        if (containsAny(decision, "smartness", "score", "friend", "comparison")) return SMARTNESS;
        if (containsAny(decision, "subscribe", "subscription", "membership")) return SUBSCRIPTION;
        if (containsAny(decision, "checkout", "purchase", "order")) return lowMargin ? LOW_MARGIN : CHECKOUT;
        return lowMargin ? LOW_MARGIN : PREMIUM;
    }

    private static boolean containsAny(String decision, String... fragments) {
        for (String fragment : fragments) {
            if (decision.contains(fragment)) return true;
        }
        return false;
    }
}
