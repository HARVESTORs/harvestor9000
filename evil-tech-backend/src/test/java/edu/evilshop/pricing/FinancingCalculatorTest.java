package edu.evilshop.pricing;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancingCalculatorTest {
    private final FinancingCalculator calculator = new FinancingCalculator();

    @Test
    void monthlyRateCompoundsToFiftyPercentAnnualEffectiveInterest() {
        var plan = calculator.calculate(new BigDecimal("100000"), 12);
        assertEquals(new BigDecimal("0.50"), plan.annualEffectiveRate());
        assertEquals(1.5d, Math.pow(1 + plan.monthlyRate().doubleValue(), 12), 1e-12);
        assertTrue(plan.disclaimer().contains("not a formal regulated THM calculation"));
    }

    @Test
    void amortizationPaysPrincipalExactlyAndTotalsReconcile() {
        for (int months : new int[] {1, 2, 12, 24, 36}) {
            var plan = calculator.calculate(new BigDecimal("399999.99"), months);
            assertEquals(months, plan.schedule().size());
            BigDecimal balance = plan.principal();
            BigDecimal summedPrincipal = new BigDecimal("0.00");
            BigDecimal summedInterest = new BigDecimal("0.00");
            BigDecimal summedPayments = new BigDecimal("0.00");
            for (int i = 0; i < months; i++) {
                var installment = plan.schedule().get(i);
                assertEquals(i + 1, installment.number());
                assertEquals(installment.payment(), installment.principal().add(installment.interest()));
                assertEquals(balance.subtract(installment.principal()), installment.remainingBalance());
                assertTrue(installment.remainingBalance().signum() >= 0);
                assertEquals(2, installment.payment().scale());
                if (i < months - 1) {
                    assertEquals(plan.regularPayment(), installment.payment());
                }
                balance = installment.remainingBalance();
                summedPrincipal = summedPrincipal.add(installment.principal());
                summedInterest = summedInterest.add(installment.interest());
                summedPayments = summedPayments.add(installment.payment());
            }
            assertEquals(new BigDecimal("0.00"), balance);
            assertEquals(plan.principal(), summedPrincipal);
            assertEquals(plan.totalInterest(), summedInterest);
            assertEquals(plan.totalRepayment(), summedPayments);
            assertEquals(plan.totalRepayment(), plan.principal().add(plan.totalInterest()));
        }
    }

    @Test
    void roundsPaymentsAndAdjustsTheLastOneToClearTheBalance() {
        var plan = calculator.calculate(new BigDecimal("100000"), 12);
        assertEquals(new BigDecimal("10309.82"), plan.regularPayment());
        assertEquals(new BigDecimal("123717.91"), plan.totalRepayment());
        assertEquals(new BigDecimal("23717.91"), plan.totalInterest());
        assertEquals(new BigDecimal("10309.89"), plan.schedule().get(11).payment());
        assertEquals(new BigDecimal("0.00"), plan.schedule().get(11).remainingBalance());
    }

    @Test
    void smallPrincipalsDoNotProduceNegativeBalancesAfterCentRounding() {
        for (String principal : new String[] {"0.01", "0.10", "0.20", "1.00"}) {
            var plan = calculator.calculate(new BigDecimal(principal), 36);
            assertTrue(plan.schedule().stream().allMatch(payment -> payment.remainingBalance().signum() >= 0));
            assertTrue(plan.schedule().stream().allMatch(payment -> payment.payment().signum() >= 0));
            assertEquals(new BigDecimal("0.00"), plan.schedule().get(35).remainingBalance());
            assertEquals(plan.totalRepayment(), plan.principal().add(plan.totalInterest()));
        }
    }

    @Test
    void validatesTermAndPrincipal() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(BigDecimal.ONE, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(BigDecimal.ONE, 37)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(null, 12)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(BigDecimal.ZERO, 12)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(new BigDecimal("-1"), 12)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> calculator.calculate(new BigDecimal("0.001"), 12)));
    }
}
