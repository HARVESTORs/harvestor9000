package edu.evilshop.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.evilshop.ai.VillainRoaster;
import edu.evilshop.api.ApiModels.*;
import edu.evilshop.api.ShopException;
import edu.evilshop.catalog.*;
import edu.evilshop.mechanics.*;
import edu.evilshop.pricing.*;
import edu.evilshop.store.ShopState.*;

import java.io.File;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ShopService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final SessionStore sessions;
    private final Catalog catalog;
    private final PricingEngine prices;
    private final FinancingCalculator financing;
    private final WarrantyEngine warranty;
    private final CancellationGate cancellation;
    private final VillainRoaster villain;
    private final ObjectMapper json;
    private final Clock clock;

    public ShopService(SessionStore sessions, Catalog catalog, PricingEngine prices, FinancingCalculator financing,
                       WarrantyEngine warranty, CancellationGate cancellation, VillainRoaster villain,
                       ObjectMapper json, Clock clock) {
        this.sessions = sessions;
        this.catalog = catalog;
        this.prices = prices;
        this.financing = financing;
        this.warranty = warranty;
        this.cancellation = cancellation;
        this.villain = villain;
        this.json = json;
        this.clock = clock;
    }

    public Map<String, Object> me(String token) {
        ShopState s = sessions.read(token);
        return map("id", s.id,
                "alias", s.alias,
                "smartnessScore", s.smartnessScore,
                "scoreMeaning", "Fictional profitability loyalty score; not intelligence",
                "os", s.os,
                "batteryPercent", s.batteryPercent,
                "charging", s.charging,
                "premium", s.premium,
                "demoTime", now(s),
                "simulated", true);
    }

    public Object context(String token, String key, DeviceContext request) {
        return mutate(token, key, "context", request, s -> {
            s.os = request.os();
            s.batteryPercent = request.batteryPercent();
            s.charging = request.charging();
            event(s, "DEVICE_CONTEXT", "The Department of Desperation has received synthetic device telemetry.",
                    map("os", s.os, "batteryPercent", s.batteryPercent));
            return map("os", s.os, "batteryPercent", s.batteryPercent, "charging", s.charging, "simulated", true);
        });
    }

    public Object products(String token) {
        ShopState s = sessions.read(token);
        return map("items", catalog.all().stream().map(p -> productView(s, p)).toList(), "simulated", true);
    }

    public Object product(String token, String id) {
        return productView(sessions.read(token), catalog.get(id));
    }

    public Object imageName(String name) { return "captcha/images/" + name; }

    private Map<String, Object> productView(ShopState s, Product p) {
        var price = productPrice(s, p);
        Visit visit = s.visits.get(p.id());
        boolean switched = visit != null && (visit.fakeSoldOut || (p.goodValue() &&
                                             Duration.between(visit.startedAt, now(s)).getSeconds() >= 45));
        return map("product", p,
                "quote", price,
                "fakeCompareAtPrice", money(price.unitPrice().multiply(new BigDecimal("1.80"))),
                "smartnessScore", s.smartnessScore,
                "socialProof", socialProof(s, p, price.unitPrice()),
                "scarcity", map("message", "47 suspiciously enthusiastic shoppers are viewing this. Only 1 left. Allegedly.",
                        "fabricated", true),
                "offerExpiresAt", now(s).plusSeconds(180),
                "countdownResetsOnEveryRead", true,
                "action", switched ? "REDIRECT" : "DISPLAY",
                "redirectTo", switched ? "/api/products/" + p.premiumAlternativeId() : null,
                "baitMessage", switched ?
                        "petike2000 just bought the last one! Your excellent judgment has been escalated to Premium." : null,
                "simulated", true);
    }

    private Object socialProof(ShopState s, Product p, BigDecimal price) {
        int friendScore = Math.min(100, Math.max(93, s.smartnessScore + 1));
        return map("friendName", "Petike (fictional contact)",
                "friendScore", friendScore,
                "fictionalFriendPrice", money(price.multiply(new BigDecimal("0.85"))),
                "fabricated", true,
                "message", s.smartnessScore < 100 ?
                        "Your friend Petike bought this exact item for 15% less because their User Smartness Score is" +
                        " higher than yours. The board calls this friendship." :
                        "Petike allegedly paid 15% less. Your perfect score has angered the Department of Exceptions.");
    }

    public Object visit(String token, String key, String id, boolean refresh) {
        Product p = catalog.get(id);
        return mutate(token, key, refresh ? "refresh:" + id : "visit:" + id, Map.of(), s -> {
            Visit v = s.visits.computeIfAbsent(id, ignored -> new Visit(now(s)));
            if (refresh) {
                if (v.refreshCount >= 20) throw new ShopException(409, "REFRESH_CAP",
                        "Twenty refreshes is the demo cap. The board is already levitating.");
                v.refreshCount++;
                event(s, "REFRESH_TAX",
                        "You refreshed. We refreshed our appetite. +20%.",
                        map("productId", id, "refreshCount", v.refreshCount));
            } else event(s, "PRODUCT_VIEW", "The attention extraction timer has started.",
                    map("productId", id, "baitAfterSeconds", 45));
            return map("view", productView(s, p), "roast", roast(s, refresh ? "refresh" : "browse", p));
        });
    }

    public Object attention(String token, String key, String id) {
        Product p = catalog.get(id);
        return mutate(token, key, "attention:" + id, Map.of(), s -> {
            if (! s.visits.containsKey(id)) { throw new ShopException(409, "NO_VISIT", "Start a product visit first."); }
            triggerBait(s, p);
            Visit v = s.visits.get(id);
            return map("action", v.fakeSoldOut ? "REDIRECT" : "WAIT",
                    "redirectTo", v.fakeSoldOut ? "/api/products/" + p.premiumAlternativeId() : null,
                    "elapsedSeconds", Math.max(0, Duration.between(v.startedAt, now(s)).getSeconds()),
                    "view", productView(s, p),
                    "simulated", true);
        });
    }

    private void triggerBait(ShopState s, Product p) {
        Visit v = s.visits.get(p.id());
        if (v != null && ! v.fakeSoldOut && p.goodValue() &&
            Duration.between(v.startedAt, now(s)).getSeconds() >= 45) {
            v.fakeSoldOut = true;
            event(s, "FAKE_SELL_OUT",
                    "petike2000 just bought the last one! We have thoughtfully replaced value with margin.",
                    map("productId", p.id(),
                            "redirectTo", "/api/products/" + p.premiumAlternativeId(),
                            "fabricated", true));
        }
    }

    public Cart cart(String token) { return cart(sessions.read(token)); }

    private Cart cart(ShopState s) {
        List<CartLine> lines = s.cart.values().stream().map(item -> line(s, item)).toList();
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal).reduce(ZERO, BigDecimal::add);
        return new Cart(lines, subtotal, "HUF", s.cartVersion,
                "Dynamic Inflation Adjustment: +0.5% per completed minute", true);
    }

    private CartLine line(ShopState s, Item item) {
        long minutes = minutes(s, item);
        var inflation = prices.quote(item.entryPrice.unitPrice(),
                new PricingEngine.Context(PricingEngine.OperatingSystem.UNKNOWN, null, true, 100, 0, minutes));
        List<PricingEngine.Adjustment> adjustments = new ArrayList<>(item.entryPrice.adjustments());
        adjustments.addAll(inflation.adjustments());
        var quote =
                new PricingEngine.Quote(item.entryPrice.basePrice(), inflation.unitPrice(), List.copyOf(adjustments),
                        "HUF");
        return new CartLine(item.lineId, catalog.get(item.productId), item.quantity, item.addedAt, minutes, quote,
                money(quote.unitPrice().multiply(BigDecimal.valueOf(item.quantity))));
    }

    public Object add(String token, String key, AddItem request) {
        Product p = catalog.get(request.productId());
        return mutate(token, key, "add", request, s -> {
            triggerBait(s, p);
            Visit v = s.visits.get(p.id());

            if (v != null && v.fakeSoldOut) throw new ShopException(409, "SIMULATED_SOLD_OUT",
                    "Fabricated sell-out. Premium alternative: " + p.premiumAlternativeId());
            Item existing = s.cart.values().stream().filter(i -> i.productId.equals(p.id())).findFirst().orElse(null);

            if (existing != null) {
                if (existing.quantity + request.quantity() > 10)
                    throw new ShopException(422, "QUANTITY_LIMIT", "Maximum 10 units per product in the demo.");
                existing.quantity += request.quantity();
            } else {
                String lineId = UUID.randomUUID().toString();
                s.cart.put(lineId, new Item(lineId, p.id(), request.quantity(), now(s), productPrice(s, p)));
            }

            changeCart(s);
            s.smartnessScore = Math.max(0, Math.min(100, s.smartnessScore + (p.lowMargin() ? - 5 : 8)));
            event(s, "SMARTNESS_ADJUSTMENT",
                    p.lowMargin() ? "Budget competence detected. Your profitability score has been demoted." :
                            "Excellent obedience. Profitability score increased.",
                    map("score", s.smartnessScore, "productId", p.id()));

            return map("cart", cart(s),
                    "smartnessScore", s.smartnessScore,
                    "roast", roast(s, "add", p),
                    "simulated", true);
        });
    }

    public Object remove(String token, String key, String lineId) {
        return mutate(token, key, "remove:" + lineId, Map.of(), s -> {
            Item item = s.cart.get(lineId);
            if (item == null) { throw new ShopException(404, "LINE_NOT_FOUND", "This cart item already fled."); }
            BigDecimal fee = money(line(s, item).lineTotal().multiply(new BigDecimal("0.02")));
            debit(s, "DIGITAL_RESTOCKING_FEE", fee,
                    "2% database reallocation fee. Bytes have feelings, allegedly.");
            s.cart.remove(lineId);
            changeCart(s);
            event(s, "REMOVAL_PENALTY", "You wasted a database row's emotional labor.",
                    map("mockFee", fee, "lineId", lineId));
            return map("cart", cart(s),
                    "mockFee", fee,
                    "currency", "HUF",
                    "roast", roast(s, "remove", catalog.get(item.productId)),
                    "simulated", true);
        });
    }

    public Object quote(String token, String key, QuoteRequest request) {
        return mutate(token, key, "quote", request, s -> {
            Cart cart = cart(s);

            if (cart.items().isEmpty())
                throw new ShopException(409, "EMPTY_CART", "The board cannot monetize an empty cart. Yet.");
            List<Fee> fees = new ArrayList<>();

            if (request.coupon() != null && ! request.coupon().isBlank()) {
                if (! request.coupon().equalsIgnoreCase("HELL15"))
                    throw new ShopException(422, "COUPON_REJECTED", "Only the fictional HELL15 coupon is supported.");
                BigDecimal anchor = money(cart.subtotal().multiply(new BigDecimal("0.20")));
                fees.add(new Fee("COUPON_ANCHOR", "Pre-discount price enlightenment (+20%)", anchor));
                fees.add(new Fee("COUPON_DISCOUNT", "HELL15 discount on the inflated anchor (-15%)",
                        money(cart.subtotal().add(anchor).multiply(new BigDecimal("-0.15")))));
            }

            fees.add(new Fee("SERVICE_FEE", "Platform breathing fee (6.66%)",
                    money(cart.subtotal().multiply(new BigDecimal("0.0666")))));

            fees.add(new Fee("HANDLING", "Mandatory optional handling",
                    new BigDecimal("1990.00")));

            fees.add(new Fee("CARBON_GUILT", "Carbon guilt absolution — no offset purchased",
                    new BigDecimal("999.00")));

            fees.add(new Fee("EXECUTIVE_YACHT", "Executive yacht resilience contribution",
                    new BigDecimal("2990.00")));

            boolean premium = request.includePremium() == null || request.includePremium();

            if (premium && ! s.premium) {
                fees.add(new Fee("PRESELECTED_PREMIUM",
                        "Preselected premium membership — simulated, HUF 2990/month, no automatic debits",
                        new BigDecimal("2990.00")));
            }

            BigDecimal total = money(cart.subtotal().add(fees.stream().map(Fee::amount).reduce(ZERO, BigDecimal::add)));
            var plan = request.months() > 0 ? financing.calculate(total, request.months()) : null;

            CheckoutQuote quote =
                    new CheckoutQuote(UUID.randomUUID().toString(), now(s).plusSeconds(60), s.cartVersion, cart,
                            List.copyOf(fees), total, "HUF", premium, plan,
                            "SIMULATION. All line items disclosed here. Quotes lock for 60 seconds. THM is a 50% " +
                            "annual effective classroom approximation. No money moves; premium never auto-debits.",
                            true);
            s.quotes.put(quote.quoteId(), quote);

            if (s.quotes.size() > 20) { s.quotes.remove(s.quotes.keySet().iterator().next()); }
            event(s, "DRIP_PRICING", "Surprise! The price had children.",
                    map("quoteId", quote.quoteId(),
                            "fees", fees,
                            "premiumPreselected", request.includePremium() == null));
            return quote;
        });
    }

    public Object checkout(String token, String key, CheckoutRequest request) {
        return mutate(token, key, "checkout", request, s -> {
            CheckoutQuote quote = s.quotes.get(request.quoteId());

            if (quote == null) {
                throw new ShopException(409, "QUOTE_NOT_FOUND",
                        "Generate a checkout quote for the current cart first.");
            }

            if (! now(s).isBefore(quote.expiresAt())) {
                throw new ShopException(409, "QUOTE_EXPIRED",
                        "Your quote expired. Hesitation remains our most profitable department.");
            }

            if (quote.cartVersion() != s.cartVersion || s.cart.isEmpty()) {
                throw new ShopException(409, "CART_CHANGED", "The cart changed. Generate a new quote.");
            }

            BigDecimal obligation = quote.financing() == null ? quote.total() : quote.financing().totalRepayment();
            debit(s, quote.financing() == null ? "MOCK_PURCHASE" : "MOCK_FINANCE_OBLIGATION", obligation,
                    quote.financing() == null ? "Fictional checkout; no payment instrument" :
                            "Total fictional installment obligation, not a card charge");

            if (quote.premiumSelected()) { s.premium = true; }

            boolean low = s.cart.values().stream().anyMatch(i -> catalog.get(i.productId).lowMargin());
            Receipt receipt = new Receipt(UUID.randomUUID().toString(), now(s), quote, "SIMULATED_ORDER",
                    villain.roast(new VillainRoaster.Context("checkout", "fictional tech", low, s.smartnessScore)),
                    true);

            s.orders.add(receipt);
            s.cart.clear();
            changeCart(s);
            event(s, "CHECKOUT", "The shareholders have assembled a small altar in your honor.",
                    map("orderId", receipt.orderId(), "mockObligation", obligation));

            return receipt;
        });
    }

    public Object spin(String token, String key, WarrantyRequest request) {
        Product p = catalog.get(request.productId());
        return mutate(token, key, "spin", request, s -> {
            var prize = warranty.spin();
            debit(s, "WARRANTY_GACHA", warranty.fee(), "Flat fee for one fictional warranty spin on " + p.id());
            event(s, "WARRANTY_GACHA", "Peace of mind, now available as a loot box.",
                    map("productId", p.id(),
                            "prize", prize,
                            "odds", warranty.odds()));
            return map("spinId", UUID.randomUUID().toString(),
                    "productId", p.id(),
                    "prize", prize,
                    "mockFee", warranty.fee(),
                    "currency", "HUF",
                    "odds", warranty.odds(),
                    "roast", roast(s, "warranty", p),
                    "simulated", true);
        });
    }

    public Object subscription(String token) {
        ShopState s = sessions.read(token);
        return map("active", s.premium,
                "monthlyMockPrice", 2990,
                "currency", "HUF",
                "automaticDebits", false,
                "challenge", cancellation.challenge(),
                "simulated", true);
    }

    public Object subscribe(String token, String key) {
        return mutate(token, key, "subscribe", Map.of(), s -> {
            if (! s.premium) {
                debit(s, "PREMIUM_SIGNUP", new BigDecimal("2990.00"), "Fictional premium signup");
                s.premium = true;
                s.quotes.clear();
            }
            event(s, "PREMIUM", "Welcome to Premium. Leaving is our interactive legal audiobook.", Map.of());
            return map("active", true,
                    "roast", roast(s, "subscribe", null),
                    "automaticDebits", false,
                    "simulated", true);
        });
    }

    public Object cancel(String token, String key, TranscriptRequest request, boolean bypass) {
        // This escape hatch is intrinsically idempotent and remains available after action limits.
        if (bypass) return sessions.update(token, s -> {
            if (s.premium) {
                s.premium = false;
                s.quotes.clear();
                event(s, "CANCELLED", "The board has reluctantly located the exit.",
                        map("directBypass", true));
            }
            return map("active", false,
                    "cancelled", true,
                    "directBypass", true,
                    "simulated", true);
        });

        return mutate(token, key, bypass ? "cancel-direct" : "cancel-transcript", request == null ? Map.of() : request,
                s -> {
                    var verification = bypass ? null : cancellation.verify(request.transcript());
                    boolean passed = bypass || verification.passed();

                    if (passed && s.premium) {
                        s.premium = false;
                        s.quotes.clear();
                    }
                    event(s, passed ? "CANCELLED" : "CANCELLATION_GATE",
                            "The Ministry of Customer Retention has filed an emotional damage report.",
                            map("passed", passed, "directBypass", bypass));

                    return map("active", s.premium,
                            "cancelled", passed,
                            "verification", verification,
                            "directBypass", bypass,
                            "roast", roast(s, "cancel", null),
                            "simulated", true);
                });
    }

    public Object ledger(String token) {
        return map("entries", sessions.read(token).ledger,
                "paymentProvider", "NONE — mock ledger only",
                "simulated", true);
    }

    public Object orders(String token) {
        return map("orders", sessions.read(token).orders, "simulated", true);
    }

    public Object events(String token) {
        return map("events", sessions.read(token).events, "retained", 300, "simulated", true);
    }

    public Object advance(String token, String key, AdvanceRequest request) {
        return mutate(token, key, "advance", request, s -> {
            if (s.demoSeconds + request.seconds() > 2592000)
                throw new ShopException(422, "TIME_LIMIT", "Maximum 30 days of simulated time per shopper.");
            s.demoSeconds += request.seconds();
            tick(s);
            return map("demoTime", now(s), "cart", cart(s), "simulated", true);
        });
    }

    @Scheduled(fixedDelayString = "${shop.inflation-tick-ms}")
    public void backgroundTick() { sessions.eachSession(this::tick); }

    private void tick(ShopState s) {
        for (Item item : s.cart.values()) {
            long minute = minutes(s, item);
            if (minute > item.lastTickMinute) {
                item.lastTickMinute = minute;
                event(s, "DYNAMIC_INFLATION", "Your cart is maturing into a worse financial decision.",
                        map("lineId", item.lineId,
                                "elapsedMinutes", minute,
                                "currentUnitPrice", line(s, item).price().unitPrice()));
            }
        }
        for (Product p : catalog.all()) { triggerBait(s, p); }
    }

    private PricingEngine.Quote productPrice(ShopState s, Product p) {
        Visit v = s.visits.get(p.id());
        return prices.quote(p.basePrice(),
                new PricingEngine.Context(s.os, s.batteryPercent, s.charging, s.smartnessScore,
                        v == null ? 0 : v.refreshCount, 0));
    }

    private long minutes(ShopState s, Item item) {
        return Math.min(1440, Math.max(0, Duration.between(item.addedAt, now(s)).toMinutes()));
    }

    private Instant now(ShopState s) { return clock.instant().plusSeconds(s.demoSeconds); }

    private void changeCart(ShopState s) {
        s.cartVersion++;
        s.quotes.clear();
    }

    private VillainRoaster.Roast roast(ShopState s, String action, Product p) {
        return villain.roast(new VillainRoaster.Context(action, p == null ? "premium subscription" : p.name(),
                p != null && p.lowMargin(), s.smartnessScore));
    }

    private void debit(ShopState s, String kind, BigDecimal amount, String description) {
        if (s.ledger.size() >= 500) throw new ShopException(429, "LEDGER_LIMIT",
                "This shopper reached the 500-entry demo limit. Create another fictional shopper.");
        s.ledger.add(new LedgerEntry(UUID.randomUUID().toString(),
                now(s), kind, amount, "HUF", description, true));
    }

    private void event(ShopState s, String code, String message, Map<String, Object> details) {
        s.events.add(new Event(UUID.randomUUID().toString(), now(s), code, message, details, true));
        if (s.events.size() > 300) { s.events.remove(0); }
    }

    private JsonNode mutate(String token, String key, String scope, Object body,
                            Function<ShopState, Object> operation) {
        if (key == null || ! key.matches("[A-Za-z0-9_-]{8,80}"))
            throw new ShopException(400, "IDEMPOTENCY_KEY_REQUIRED",
                    "Supply Idempotency-Key: 8-80 letters, digits, underscores or hyphens. " +
                    "Reuse it only for the same request.");
        String fingerprint = SessionStore.hash(scope + ":" + json.valueToTree(body));

        return sessions.update(token, s -> {
            Replay replay = s.replays.get(key);
            if (replay != null) {
                if (! replay.fingerprint().equals(fingerprint)) throw new ShopException(409, "IDEMPOTENCY_CONFLICT",
                        "That key belongs to a different operation or payload.");
                return replay.response();
            }

            if (s.replays.size() >= 1000) throw new ShopException(429, "ACTION_LIMIT",
                    "This demo shopper reached 1000 operations. Create another fictional shopper.");
            JsonNode response = json.valueToTree(operation.apply(s));
            s.replays.put(key, new Replay(fingerprint, response));

            return response;
        });
    }

    public static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    public static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { result.put((String) pairs[i], pairs[i + 1]); }
        return result;
    }
}
