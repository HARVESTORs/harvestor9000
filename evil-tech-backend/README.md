# EvilTech — Department of Margin Extraction

A Java backend for a university dark-pattern demonstration. Six fictional laptops and phones. Twenty implemented manipulation mechanics. A local cartoon CEO named **MARGIN** who regards a sensible purchase as a personal attack.

**This is a simulation:** every transaction is an entry in a mock ledger, contacts and reviews are invented, the cancellation gate accepts a synthetic transcript, and the API advertises `simulated: true`. There is no card processor, contacts import, microphone capture, biometric identification, external AI, or Grok dependency. Runtime behavior uses no external services.

## Run

Requires **JDK 17 or later**. Build with Maven 3.6.3+ or the included checksum-pinned bootstrap (`curl`, `tar`, and `sha512sum`/`shasum` required on first use). The first source build downloads dependencies from Maven Central.

```sh
./mvnw test
./mvnw package
java -jar target/evil-tech-backend-1.0.0.jar
```

On Windows, use an installed Maven: `mvn test`, `mvn package`, then the same `java -jar` command. A prebuilt executable JAR is also delivered beside this source folder; it requires only Java, with no dependency downloads.

The API listens at `http://127.0.0.1:8080`. It stores demo state in `./data/evilshop.mv.db`, relative to the working directory. Stop with Ctrl+C. All demo state survives restarts; stop the server and delete that **demo data directory** to reset it. Do not run multiple application instances against the same database file.

Run the scripted presentation while the server is running:

```sh
python3 scripts/demo.py
```

It demonstrates context pricing, refresh markup, bait-and-switch, timed cart inflation, a removal penalty, coupon manipulation, financing, checkout, warranty gacha, cancellation, and the ledger. It advances simulated time so the demo completes without waiting minutes. Each run creates its own fictional shopper.

## The mechanics

| Mechanic | Implemented behavior |
| --- | --- |
| OS margin | macOS/iOS +15%, Windows +5%, Android +8%; Linux/unknown no OS markup |
| Battery desperation | +15% at 0–20% synthetic battery, unless charging |
| User Smartness Score | Fictional profitability score, starts at 50. Low-margin additions −5, high-margin additions +8; clamp 0–100. Surcharge is `(100 − score) × 0.2%`. This does not assess intelligence. |
| Contact guilt | Fictional Petike claims a 15% lower price and usually a higher score |
| Refresh punishment | +20% compounded for each explicit refresh; 20-refresh cap |
| Quantum cart | +0.5% compounded per completed minute; 24-hour inflation cap |
| Bait-and-switch | After 45 seconds viewing a value SKU, pretend `petike2000` bought the last one; return a premium redirect action |
| Fake scarcity | Invented viewer count and remaining stock |
| Resetting countdown | Fabricated three-minute offer restarts on every product read |
| Anchor inflation | Fake comparison price is 1.8× the current quote |
| Decoy products | Inferior alternatives priced close to the premium models |
| Digital restocking | Removing a cart line records 2% of its current line total as a mock fee |
| Drip pricing | 6.66% platform breathing fee; HUF 1,990 handling, 999 carbon guilt, 2,990 executive yacht contribution |
| Coupon trap | `HELL15` raises the merchandise anchor 20%, then discounts that inflated figure 15%; net merchandise increase 2% |
| Preselected premium | Quote defaults to premium unless `includePremium:false`; signup HUF 2,990. No automatic monthly debits. Existing members are not charged another signup fee. |
| Installments | 1–36 months at 50% annual effective interest; full amortization schedule. Classroom THM approximation, not a legally specified THM disclosure. |
| Warranty gacha | HUF 4,990/spin: 80% 30 days, 19% one year, 1% lifetime. Damage outside shipping excluded. |
| Cancellation obstruction | Exact 500-word transcript hurdle, ignoring case/punctuation. A direct cancellation escape hatch is always available. |
| Confirmshaming | Deterministic local villain lines for unprofitable choices and other actions |
| Fake reviews | Three clearly tagged invented reviews and an invented satisfaction statistic |

“Every dark practice” is open-ended; these twenty mechanics are the concrete implemented scope. `/api/mechanisms` is the machine-readable inventory.

## API usage

[OpenAPI specification](docs/openapi.yaml) describes requests, responses, errors, and examples. Import it into Postman or an OpenAPI viewer. No hosted Swagger UI or storefront is included; this project is the backend.

Create a fictional shopper:

```sh
curl -s http://127.0.0.1:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"alias":"Demo Student","simulationAcknowledged":true}'
```

Copy the returned `token`. Shopper routes require `Authorization: Bearer <token>`. Every mutation also requires a unique **Idempotency-Key** of 8–80 letters, numbers, `_` or `-`, except session creation and direct cancellation. Retrying the same operation with the same key returns its original result; reusing a key for a different request returns 409. Never reuse the key for a new action, including a fresh attention poll.

```sh
curl -s http://127.0.0.1:8080/api/cart/items \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -H 'Idempotency-Key: add-demo-001' \
  -H 'Content-Type: application/json' \
  -d '{"productId":"laptop-sensible","quantity":1}'
```

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/health`, `/api/mechanisms`, `/api/propaganda` | Public status, mechanism inventory, fabricated reviews |
| POST | `/api/sessions` | Create shopper; keep returned bearer token |
| GET | `/api/me` | Shopper score and synthetic context |
| PUT | `/api/context` | Set `os`, nullable `batteryPercent` (0–100), `charging` |
| GET | `/api/products`, `/api/products/{id}` | Catalog, quotes, persuasion metadata |
| POST | `/api/products/{id}/visit` | Start server-side attention timer |
| POST | `/api/products/{id}/refresh` | Apply one 20% refresh increment |
| POST | `/api/products/{id}/attention` | Return `WAIT` or `REDIRECT` based on server time |
| GET | `/api/cart` | Current calculated cart |
| POST | `/api/cart/items` | Add `{productId, quantity}` |
| DELETE | `/api/cart/items/{lineId}` | Remove line and record mock 2% fee |
| POST | `/api/checkout/quote` | `{months:0..36, coupon?:"HELL15", includePremium?:false}`; zero months means pay in full |
| POST | `/api/checkout` | Commit `{quoteId}` once; mock payment/finance obligation only |
| GET | `/api/orders`, `/api/ledger`, `/api/events` | Receipts, fictional obligations, last 300 audit events |
| POST | `/api/warranty/spin` | `{productId}`; one paid mock spin per operation |
| GET | `/api/subscription` | State and exact cancellation disclaimer |
| POST | `/api/subscription` | Activate mock premium |
| POST | `/api/subscription/cancel` | `{transcript}`; exact normalized transcript required |
| POST | `/api/subscription/cancel-direct` | Immediate cancellation; no transcript or idempotency key needed |
| POST | `/api/demo/advance` | Advance this shopper's clock by `{seconds:1..86400}` |

Product IDs: `laptop-sensible`, `laptop-overlord`, `laptop-decoy`, `phone-sensible`, `phone-emperor`, `phone-decoy`. Device enum: `MACOS`, `IOS`, `WINDOWS`, `ANDROID`, `LINUX`, `UNKNOWN`.

## Frontend integration semantics

The backend cannot detect a browser's battery, control its microphone, or navigate it directly. For this lab, submit **synthetic** device values to `/api/context`. All such inputs are self-reported, not trusted telemetry.

When a product screen opens, call `/visit` once. On an actual reload, call `/refresh` with a new key. Reads never increase the refresh tax. At 45 seconds, call `/attention`; an `action:"REDIRECT"` response includes `redirectTo`, which a frontend can map to its premium product page. No actual inventory is depleted. Catalog reads also report a due redirect even between scheduler ticks.

Cart entries lock device, score and refresh pricing when first added. Later quantity increases retain the original entry quote and age. Only cart inflation changes that entry's price over time. A background job records inflation/attention events every minute; all cart reads independently derive the correct completed-minute price from server timestamps, so missed ticks and restarts do not lose or double-charge time.

Checkout first creates a quote containing **every** fee and the payment schedule. A quote locks the displayed price for 60 seconds. Cart or premium membership changes invalidate it. Checkout uses only the stored quote; clients cannot submit prices. `includePremium:false` avoids a new membership and does not cancel an existing one. Financing records the **total obligation**, not a debit today, and does not schedule real or mock monthly charges.

The warranty endpoint is a standalone fictional spin for a catalog SKU; it does not issue a real warranty contract. The 500-word gate checks text only and stores no raw transcript. Direct cancellation is independent of operation limits.

## Implementation and verification

Spring Boot 3.5.16, Java 17 language target, JDBC, file-backed H2, Jackson, Jakarta validation, JUnit 5, MockMvc. `Laptop` and `Phone` implement a sealed `Product` interface with CPU, RAM, SSD/storage and device-specific attributes.

`SessionStore` persists one JSON aggregate per shopper. A database row lock wraps each mutation, including random warranty outcomes, mock debits, audit events and idempotency records. Failures roll the transaction back. Tokens are randomly generated and only their SHA-256 hashes are stored. All currency calculations use decimal money with two decimal places and HALF_UP rounding. Compounded multipliers use bounded precision to keep old carts serializable.

Tests cover pricing composition, rounding, loan amortization, every warranty roll, exact transcript matching, API validation/authentication, timer boundaries, persistence round trips, replay conflicts, concurrent duplicate requests, quote expiry, premium/quote invalidation, and rollback. See `VERIFICATION.md` for the executed results.

Demo limits: 1,000 shopper sessions, 1,000 replayable operations per shopper, 500 ledger entries, 10 units per SKU, 20 retained quotes, last 300 events, 30 days total clock advancement, 32 KiB request bodies. These bounds keep the local classroom exercise manageable. This is a single-instance teaching backend, not a production commerce platform.

Environment: `PORT` (8080), `BIND_ADDRESS` (127.0.0.1), `DB_URL` (the file-backed H2 JDBC URL). No secrets or API keys are needed. For a container, the included Dockerfile builds the application; publish only to loopback for a local demo, for example `-p 127.0.0.1:8080:8080`. Container execution is optional.

Framework compatibility reference: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
