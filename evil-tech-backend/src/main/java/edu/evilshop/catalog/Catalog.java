package edu.evilshop.catalog;

import edu.evilshop.api.ShopException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Catalog {
    private final List<Product> products = List.of(
        new Laptop("laptop-sensible", "SensibleBook 14 — shareholder disappointment", "Ryzen 5 class / fictional", 16, 512, "Integrated", 14, m("249990"), m("225000"), true, "laptop-overlord"),
        new Laptop("laptop-overlord", "OverlordBook Ultra — now with executive aura", "Core Ultra 9 class / fictional", 32, 2048, "Premium discrete / fictional", 16, m("899990"), m("360000"), false, null),
        new Laptop("laptop-decoy", "DecoyBook Pro — almost the same price, fewer things", "Core Ultra 5 class / fictional", 16, 512, "Integrated", 15.6, m("849990"), m("280000"), false, "laptop-overlord"),
        new Phone("phone-sensible", "PocketSense — alarmingly reasonable", "Octa-core value / fictional", 8, 256, 5000, 6.1, m("129990"), m("119000"), true, "phone-emperor"),
        new Phone("phone-emperor", "EmperorPhone Obsidian — a status subscription", "Flagship silicon / fictional", 16, 1024, 5100, 6.8, m("599990"), m("210000"), false, null),
        new Phone("phone-decoy", "Phone of Questionable Value", "Midrange silicon / fictional", 8, 128, 4200, 6.4, m("579990"), m("170000"), false, "phone-emperor")
    );
    private static BigDecimal m(String n) { return new BigDecimal(n).setScale(2); }
    public List<Product> all() { return products; }
    public Product get(String id) { return products.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow(() -> new ShopException(404, "PRODUCT_NOT_FOUND", "That product escaped the boardroom.")); }
}
