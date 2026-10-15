package edu.evilshop.store;

import com.fasterxml.jackson.databind.JsonNode;
import edu.evilshop.api.ApiModels.CheckoutQuote;
import edu.evilshop.api.ApiModels.Receipt;
import edu.evilshop.pricing.PricingEngine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** One aggregate per synthetic shopper; all changes are committed under one database row lock. */
public class ShopState {
    public String id;
    public String alias;
    public Instant createdAt;
    public PricingEngine.OperatingSystem os = PricingEngine.OperatingSystem.UNKNOWN;
    public Integer batteryPercent;
    public boolean charging;
    public int smartnessScore = 50;
    public long demoSeconds;
    public long cartVersion;
    public boolean premium;
    public Map<String, Visit> visits = new LinkedHashMap<>();
    public Map<String, Item> cart = new LinkedHashMap<>();
    public Map<String, CheckoutQuote> quotes = new LinkedHashMap<>();
    public List<Receipt> orders = new ArrayList<>();
    public List<LedgerEntry> ledger = new ArrayList<>();
    public List<Event> events = new ArrayList<>();
    public Map<String, Replay> replays = new LinkedHashMap<>();

    public static class Visit {
        public Instant startedAt;
        public int refreshCount;
        public boolean fakeSoldOut;
        public Visit() {}
        public Visit(Instant time) { startedAt = time; }
    }

    public static class Item {
        public String lineId;
        public String productId;
        public int quantity;
        public Instant addedAt;
        public PricingEngine.Quote entryPrice;
        public long lastTickMinute;
        public Item() {}
        public Item(String id, String product, int amount, Instant time, PricingEngine.Quote price) {
            lineId=id; productId=product; quantity=amount; addedAt=time; entryPrice=price;
        }
    }

    public record LedgerEntry(String id,
                              Instant at,
                              String kind,
                              BigDecimal amount,
                              String currency,
                              String description,
                              boolean simulated) {}

    public record Event(String id,
                        Instant at,
                        String code,
                        String message,
                        Map<String, Object> details,
                        boolean simulated) {}

    public record Replay(String fingerprint,
                         JsonNode response) {}
}
