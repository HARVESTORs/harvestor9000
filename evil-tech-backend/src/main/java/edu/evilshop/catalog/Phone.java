package edu.evilshop.catalog;
import java.math.BigDecimal;

public record Phone(String id, String name, String cpu, int ramGb, int storageGb,
                    int batteryMah, double displayInches, BigDecimal basePrice, BigDecimal costPrice,
                    boolean goodValue, String premiumAlternativeId) implements Product {}
