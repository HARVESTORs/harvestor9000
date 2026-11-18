package edu.evilshop;

import edu.evilshop.ai.VillainRoaster;
import edu.evilshop.mechanics.CancellationGate;
import edu.evilshop.mechanics.WarrantyEngine;
import edu.evilshop.pricing.FinancingCalculator;
import edu.evilshop.pricing.PricingEngine;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EvilShopApplication {
    public static void main(String[] args) { SpringApplication.run(EvilShopApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PricingEngine pricingEngine() { return new PricingEngine(); }
    @Bean FinancingCalculator financingCalculator() { return new FinancingCalculator(); }
    @Bean WarrantyEngine warrantyEngine() { return new WarrantyEngine(); }
    @Bean CancellationGate cancellationGate() { return new CancellationGate(); }
    @Bean VillainRoaster villainRoaster() { return new VillainRoaster(); }
}
