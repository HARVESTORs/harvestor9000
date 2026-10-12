package edu.evilshop.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Amortization illustration using 50% annual effective interest and no fees. */
public final class FinancingCalculator {
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal ANNUAL_EFFECTIVE_RATE = new BigDecimal("0.50");
    private static final BigDecimal MONTHLY_RATE =
            BigDecimal.valueOf(StrictMath.pow(1.5d, 1.0d / 12.0d) - 1.0d);
    private static final String DISCLAIMER =
            "Educational simulation: 50% annual effective interest with no fees; "
            + "this is not a formal regulated THM calculation.";

    public record Installment(
            int number,
            BigDecimal payment,
            BigDecimal interest,
            BigDecimal principal,
            BigDecimal remainingBalance) {}

    public record Plan(
            BigDecimal principal,
            BigDecimal annualEffectiveRate,
            BigDecimal monthlyRate,
            int months,
            BigDecimal regularPayment,
            BigDecimal totalRepayment,
            BigDecimal totalInterest,
            List<Installment> schedule,
            String disclaimer) {
        public Plan {
            schedule = List.copyOf(schedule);
        }
    }

    public Plan calculate(BigDecimal principal, int months) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (months < 1 || months > 36) {
            throw new IllegalArgumentException("Installment term must be between 1 and 36 months");
        }
        BigDecimal initialPrincipal = money(principal);
        if (initialPrincipal.signum() <= 0) {
            throw new IllegalArgumentException("Principal must round to at least 0.01 HUF");
        }

        BigDecimal compound = BigDecimal.ONE.add(MONTHLY_RATE).pow(months, PRECISION);
        BigDecimal regularPayment = money(initialPrincipal.multiply(MONTHLY_RATE, PRECISION)
                .multiply(compound, PRECISION)
                .divide(compound.subtract(BigDecimal.ONE), PRECISION));
        BigDecimal balance = initialPrincipal;
        BigDecimal totalRepayment = money(BigDecimal.ZERO);
        BigDecimal totalInterest = money(BigDecimal.ZERO);
        List<Installment> schedule = new ArrayList<>(months);

        for (int number = 1; number <= months; number++) {
            BigDecimal interest = money(balance.multiply(MONTHLY_RATE, PRECISION));
            BigDecimal payoff = balance.add(interest);
            // A tiny principal can be repaid early because payments round to cents.
            BigDecimal payment = number == months ? payoff : regularPayment.min(payoff);
            BigDecimal principalPart = payment.subtract(interest);
            balance = balance.subtract(principalPart);
            totalRepayment = totalRepayment.add(payment);
            totalInterest = totalInterest.add(interest);
            schedule.add(new Installment(number, payment, interest, principalPart, balance));
        }

        return new Plan(initialPrincipal, ANNUAL_EFFECTIVE_RATE, MONTHLY_RATE, months,
                regularPayment, totalRepayment, totalInterest, schedule, DISCLAIMER);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
