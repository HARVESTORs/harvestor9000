package edu.evilshop.api;

import edu.evilshop.ai.VillainRoaster.Roast;
import edu.evilshop.catalog.Product;
import edu.evilshop.pricing.FinancingCalculator.Plan;
import edu.evilshop.pricing.PricingEngine;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}
    public record CreateSession(@NotBlank @Pattern(regexp="[A-Za-z0-9 _-]{1,32}") String alias,
                                @AssertTrue(message="The classroom simulation must be acknowledged") boolean simulationAcknowledged) {}
    public record DeviceContext(@NotNull PricingEngine.OperatingSystem os, @Min(0) @Max(100) Integer batteryPercent, boolean charging) {}
    public record AddItem(@NotBlank String productId, @Min(1) @Max(19990) int quantity) {}
    public record QuoteRequest(@Min(0) @Max(36) int months, @Size(max=24) String coupon, Boolean includePremium) {}
    public record CheckoutRequest(@NotBlank String quoteId) {}
    public record WarrantyRequest(@NotBlank String productId) {}
    public record TranscriptRequest(@NotBlank @Size(max=999999) String transcript) {}
    public record AdvanceRequest(@Min(1) @Max(9999999) long seconds) {}
    public record CartLine(String lineId, Product product, int quantity, Instant addedAt, long elapsedMinutes,
                           PricingEngine.Quote price, BigDecimal lineTotal) {}
    public record Cart(List<CartLine> items, BigDecimal subtotal, String currency, long version, String inflationLabel, boolean simulated) {}
    public record Fee(String code, String label, BigDecimal amount) {}
    public record CheckoutQuote(String quoteId, Instant expiresAt, long cartVersion, Cart cart,
                                List<Fee> fees, BigDecimal total, String currency, boolean premiumSelected,
                                Plan financing, String disclaimer, boolean simulated) {}
    public record Receipt(String orderId, Instant createdAt, CheckoutQuote quote, String status, Roast roast, boolean simulated) {}
}
