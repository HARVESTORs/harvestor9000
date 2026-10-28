package edu.evilshop.api;

import edu.evilshop.api.ApiModels.*;
import edu.evilshop.store.SessionStore;
import edu.evilshop.store.ShopService;
import jakarta.validation.Valid;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static edu.evilshop.store.ShopService.map;

@RestController
public class ShopController {
    private final ShopService shop;
    private final SessionStore sessions;

    public ShopController(ShopService shop, SessionStore sessions) {
        this.shop = shop;
        this.sessions = sessions;
    }

    @GetMapping({"/", "/api/health"})
    public Object health() {
        return map("status", "UP", "name", "EvilTech — Department of Margin Extraction", "mode",
                "UNIVERSITY_SIMULATION", "currency", "HUF", "ai", "LOCAL_VILLAIN", "payments", "MOCK_ONLY",
                "mechanisms", "/api/mechanisms", "simulated", true);
    }

    @PostMapping("/api/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Object session(@Valid @RequestBody CreateSession request) { return sessions.create(request.alias()); }

    @GetMapping("/api/me")
    public Object me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.me(token(auth));
    }

    @GetMapping("/api/captcha/images/{file}")
    public Object image(@PathVariable String file) {
        return shop.imageName(file);
    }

    @PutMapping("/api/context")
    public Object context(@RequestHeader(value = "Authorization", required = false) String auth,
                          @RequestHeader(value = "Idempotency-Key", required = false) String key,
                          @Valid @RequestBody DeviceContext request) { return shop.context(token(auth), key, request); }

    @GetMapping("/api/products")
    public Object products(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.products(token(auth));
    }

    @GetMapping("/api/products/{id}")
    public Object product(@RequestHeader(value = "Authorization", required = false) String auth,
                          @PathVariable String id) { return shop.product(token(auth), id); }

    @PostMapping("/api/products/{id}/visit")
    public Object visit(@RequestHeader(value = "Authorization", required = false) String auth,
                        @RequestHeader(value = "Idempotency-Key", required = false) String key,
                        @PathVariable String id) { return shop.visit(token(auth), key, id, false); }

    @PostMapping("/api/products/{id}/refresh")
    public Object refresh(@RequestHeader(value = "Authorization", required = false) String auth,
                          @RequestHeader(value = "Idempotency-Key", required = false) String key,
                          @PathVariable String id) { return shop.visit(token(auth), key, id, true); }

    @PostMapping("/api/products/{id}/attention")
    public Object attention(@RequestHeader(value = "Authorization", required = false) String auth,
                            @RequestHeader(value = "Idempotency-Key", required = false) String key,
                            @PathVariable String id) { return shop.attention(token(auth), key, id); }

    @GetMapping("/api/cart")
    public Object cart(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.cart(token(auth));
    }

    @PostMapping("/api/cart/items")
    public Object add(@RequestHeader(value = "Authorization", required = false) String auth,
                      @RequestHeader(value = "Idempotency-Key", required = false) String key,
                      @Valid @RequestBody AddItem request) { return shop.add(token(auth), key, request); }

    @DeleteMapping("/api/cart/items/{lineId}")
    public Object remove(@RequestHeader(value = "Authorization", required = false) String auth,
                         @RequestHeader(value = "Idempotency-Key", required = false) String key,
                         @PathVariable String lineId) { return shop.remove(token(auth), key, lineId); }

    @PostMapping("/api/checkout/quote")
    public Object quote(@RequestHeader(value = "Authorization", required = false) String auth,
                        @RequestHeader(value = "Idempotency-Key", required = false) String key,
                        @Valid @RequestBody QuoteRequest request) { return shop.quote(token(auth), key, request); }

    @PostMapping("/api/checkout")
    public Object checkout(@RequestHeader(value = "Authorization", required = false) String auth,
                           @RequestHeader(value = "Idempotency-Key", required = false) String key,
                           @Valid @RequestBody CheckoutRequest request) {
        return shop.checkout(token(auth), key, request);
    }

    @GetMapping("/api/orders")
    public Object orders(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.orders(token(auth));
    }

    @PostMapping("/api/warranty/spin")
    public Object spin(@RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestHeader(value = "Idempotency-Key", required = false) String key,
                       @Valid @RequestBody WarrantyRequest request) { return shop.spin(token(auth), key, request); }

    @GetMapping("/api/subscription")
    public Object subscription(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.subscription(token(auth));
    }

    @PostMapping("/api/subscription")
    public Object subscribe(@RequestHeader(value = "Authorization", required = false) String auth,
                            @RequestHeader(value = "Idempotency-Key", required = false)
                            String key) { return shop.subscribe(token(auth), key); }

    @PostMapping("/api/subscription/cancel")
    public Object cancel(@RequestHeader(value = "Authorization", required = false) String auth,
                         @RequestHeader(value = "Idempotency-Key", required = false) String key,
                         @Valid @RequestBody TranscriptRequest request) {
        return shop.cancel(token(auth), key, request, false);
    }

    @PostMapping("/api/subscription/cancel-direct")
    public Object cancelDirect(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.cancel(token(auth), null, null, true);
    }

    @GetMapping("/api/ledger")
    public Object ledger(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.ledger(token(auth));
    }

    @GetMapping("/api/events")
    public Object events(@RequestHeader(value = "Authorization", required = false) String auth) {
        return shop.events(token(auth));
    }

    @PostMapping("/api/demo/advance")
    public Object advance(@RequestHeader(value = "Authorization", required = false) String auth,
                          @RequestHeader(value = "Idempotency-Key", required = false) String key,
                          @Valid @RequestBody AdvanceRequest request) {
        return shop.advance(token(auth), key, request);
    }

    @GetMapping("/api/propaganda")
    public Object propaganda() {
        return map("reviews", List.of(map("alias", "DefinitelyNotTheCEO", "stars", 5, "text",
                                "My wallet is empty, but the yacht looks incredible.", "fabricated", true),
                        map("alias", "TotallyIndependentConsumer", "stars", 5, "text",
                                "I tried to cancel. Now I have a minor in contract law.", "fabricated", true),
                        map("alias", "petike2000", "stars", 5, "text",
                                "I bought the last unit of everything, " + "simultaneously.", "fabricated", true)),
                "banner",
                "97% of imaginary customers recommend surrendering.", "simulated", true);
    }

    @GetMapping("/api/mechanisms")
    public Object mechanisms() {
        return map("mode", "SATIRICAL_CLASSROOM_LAB", "implemented",
                List.of(rule("OS_MARGIN", "MACOS/IOS +15%, Windows +5%, Android +8%", "/api/context"),
                        rule("LOW_BATTERY", "Unplugged battery <=20% adds 15%", "/api/context"),
                        rule("SMARTNESS_SCORE", "Profitability loyalty score; cheap choices lose points", "/api/me"),
                        rule("CONTACT_GUILT", "Fictional Petike allegedly paid 15% less", "/api/products"),
                        rule("REFRESH_TAX", "Each explicit refresh compounds +20%; cap20",
                                "/api/products/{id}/refresh"),
                        rule("QUANTUM_CART", "+0.5% per complete minute; cap1440 minutes", "/api/cart"),
                        rule("FAKE_SELL_OUT", "45 seconds on value products triggers premium redirect",
                                "/api/products/{id}/attention"),
                        rule("FAKE_SCARCITY", "Fabricated viewer and stock counts", "/api/products"),
                        rule("RESETTING_COUNTDOWN", "Three-minute countdown resets on every read", "/api/products"),
                        rule("INFLATED_ANCHOR", "Fake compare-at price is 1.8 times current price", "/api/products"),
                        rule("DECOY_PRICING", "Inferior products positioned close to premium prices", "/api/products"),
                        rule("REMOVAL_PENALTY", "2% mock database reallocation charge", "/api/cart/items/{lineId}"),
                        rule("DRIP_FEES", "Breathing, handling, carbon guilt and yacht fees", "/api/checkout/quote"),
                        rule("FAKE_COUPON", "HELL15 applies +20% before -15%; net +2%", "/api/checkout/quote"),
                        rule("PRESELECTED_PREMIUM", "Premium selected unless explicitly false; no automatic debits",
                                "/api/checkout/quote"),
                        rule("FINANCING", "50% annual effective interest approximation", "/api/checkout/quote"),
                        rule("WARRANTY_GACHA", "80%30-day / 19%one-year / 1%lifetime; shipping-only exclusion",
                                "/api/warranty/spin"),
                        rule("CANCELLATION_GATE", "Exact 500-word transcript hurdle, with direct escape hatch",
                                "/api/subscription"),
                        rule("CONFIRM_SHAMING", "Local villain ridicules unprofitable decisions", "/api/cart/items"),
                        rule("FAKE_REVIEWS", "Clearly tagged invented testimonials", "/api/propaganda")), "simulated",
                true);
    }

    private Object rule(String code, String behavior, String endpoint) {
        return map("code", code, "behavior", behavior, "endpoint", endpoint);
    }

    static String token(String header) {
        if (header == null || ! header.matches("(?i)Bearer [A-Za-z0-9_-]{43}"))
            throw new ShopException(401, "MISSING_SESSION",
                    "Use Authorization: Bearer <token> from POST /api/sessions.");
        return header.substring(7);
    }
}
