package edu.evilshop.catalog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Laptop.class, name = "LAPTOP"),
               @JsonSubTypes.Type(value = Phone.class, name = "PHONE")})

public sealed interface Product permits Laptop, Phone {
    @JsonProperty("type")
    default String type() { return this instanceof Laptop ? "LAPTOP" : "PHONE"; }
    String id();
    String name();
    String cpu();
    int ramGb();
    BigDecimal basePrice();
    BigDecimal costPrice();
    boolean goodValue();
    String premiumAlternativeId();
    default boolean lowMargin() { return basePrice().subtract(costPrice()).divide(basePrice(), 4,
            java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal("0.20")) < 0; }
}
