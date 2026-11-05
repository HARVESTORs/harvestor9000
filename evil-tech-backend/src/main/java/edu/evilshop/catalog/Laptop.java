package edu.evilshop.catalog;
import java.math.BigDecimal;

public record Laptop(String id, String name, String cpu, int ramGb, int ssdGb,
                     String gpu, double displayInches, BigDecimal basePrice, BigDecimal costPrice,
                     boolean goodValue, String premiumAlternativeId) implements Product {}
