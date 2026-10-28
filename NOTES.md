# Developer Notes: EvilTech (harvestor9000)

No corporate fluff, just what you need to know about this repo.

---

## What is this thing?

It's a Spring Boot 3 / Java 17 backend built to simulate every sleazy dark pattern an e-commerce site could possibly pull off.
Think dynamic price gouging, fake urgency counters, drip pricing, and hostile cancellation gates.

Everything is completely simulated:
- Payments are fake ledger entries.
- Reviews and stock counts are made up on the fly.
- Database is a local H2 file (`evil-tech-backend/data/evilshop.mv.db`).

---

## Quickstart (TL;DR)

Run from repo root:

```bash
# 1. Start backend (compiles first if JAR doesn't exist)
./run.sh start

# 2. Open the UI in your browser
# http://127.0.0.1:8080/index.html

# 3. Run the automated demo script (in a separate terminal)
./run.sh demo
```

---

## Heads-up / Known Repo Traps

1. **Root directory vs backend directory:**
   All Java code, maven wrapper (`mvnw`), and `pom.xml` live inside `evil-tech-backend/`. If you run `./mvnw` from repo root without using `./run.sh`, bash will yell at you. Either use `./run.sh` or `cd evil-tech-backend`.

2. **The unit test failures (`PricingEngineTest`):**
   If you run raw `./mvnw test`, you will see 3 test failures in `PricingEngineTest`.
   Why? Someone tweaked `PricingEngine.java`:
   - `MACOS` / `IOS` multiplier was changed to `55.15` (a 5415% margin on Apple users instead of the normal 15%).
   - Refresh count bound was changed to 2000 and cart minutes to 999999.
   The tests expect the standard simulation values (`1.15`, 20, 1440).
   If you want to package the JAR, use `-DskipTests` (or just use `./run.sh build`).

3. **Database Reset:**
   If you mess up your test cart or want a fresh start, kill the server and delete the data directory:
   ```bash
   ./run.sh clean-db
   ```
   Or manually: `rm -rf evil-tech-backend/data`

4. **Idempotency keys:**
   POST and DELETE endpoints require an `Idempotency-Key` header with 8 to 80 characters. If you reuse the same key with different payload, the API will hit you with `409 Conflict`. If you retry with the exact same payload, it safely replays the previous result.

---

## The Dark Patterns in Plain English

1. **Apple Tax:** macOS / iOS users get hit with extra margin.
2. **Battery Desperation:** If your battery is under 20% and not charging, prices jump 15%.
3. **Smartness Score:** If you buy high-margin junk, you look "smart" to CEO MARGIN. If you buy sensible stuff, your score drops and prices go up.
4. **Petike:** A fictional shopper who always got a 15% cheaper deal than you.
5. **F5 Penalty:** Refreshing the product page stacks +20% on the price every single time.
6. **Quantum Cart:** Every minute an item sits in your cart, it gets 0.5% more expensive.
7. **Bait & Switch:** Stare at a cheap item for 45 seconds? "Oops, petike2000 just bought the last one!" -> redirects to expensive model.
8. **Fake Stock / Viewers:** "14 people looking at this right now! Only 2 left!" (All randomized fake numbers).
9. **Fake 3-Minute Timer:** A countdown timer that resets every time you reload.
10. **Anchor Inflation:** The crossed-out "original price" is just 1.8x the quoted price.
11. **Decoys:** Selling a trash phone for almost the price of the flagship to make the flagship seem like a deal.
12. **Restocking Fine:** Delete something from your cart? 2% penalty added to your bill.
13. **Drip Pricing at Checkout:**
    - 6.66% "Platform breathing fee"
    - 1,990 HUF "Handling fee"
    - 999 HUF "Carbon guilt"
    - 2,990 HUF "Executive yacht contribution"
14. **Coupon Trap (`HELL15`):** Inflates base price by 20%, then gives you 15% off. You end up paying 2% more than without the coupon.
15. **Sneaky Subscription:** Auto-checks a 2,990 HUF monthly VIP subscription at checkout.
16. **Predatory Financing:** 50% APR installment plan.
17. **Warranty Gacha:** Pay 4,990 HUF to spin a wheel for warranty. 80% chance you get 30 days. Damage outside transit excluded.
18. **500-Word Cancellation Essay:** You must type out the exact legal disclaimer word-for-word to cancel your VIP membership.
19. **Escape Hatch:** There is a secret direct cancel button (`/api/subscription/cancel-direct`) that skips the essay.
20. **CEO MARGIN Quotes:** Sarcastic roasts whenever you try to be responsible with your money.

---

## Quick API Test

Create a session and grab your token:
```bash
curl -s http://127.0.0.1:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"alias":"Tester","simulationAcknowledged":true}'
```

Set fake context (low battery on macOS):
```bash
curl -s -X PUT http://127.0.0.1:8080/api/context \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: ctx-001' \
  -d '{"os":"MACOS","batteryPercent":10,"charging":false}'
```
