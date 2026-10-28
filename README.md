# EvilTech — Department of Margin Extraction

A university dark-pattern simulator built with Spring Boot (Java 17). It models an aggressively anti-consumer electronics store with dynamic context pricing, artificial urgency, drip fees, deceptive discounts, and an obstructive cancellation flow.

> **Simulated environment:** All purchases, mock ledger entries, product reviews, and customer scores are purely fictional. No real payment gateways, hardware telemetry, biometric tracking, or external services are used.

---

## Quick Start

You can use the root helper script or standard Maven commands:

### Option A: Using the helper script

```bash
# Build & start backend (listens on http://127.0.0.1:8080)
./run.sh start

# In another terminal, run the scripted demo walkthrough
./run.sh demo

# Reset the local mock database if needed
./run.sh clean-db
```

### Option B: Using Maven directly

```bash
cd evil-tech-backend

# Build the JAR (skips unit tests)
./mvnw package -DskipTests

# Run the backend
java -jar target/evil-tech-backend-1.0.0.jar
```

---

## Storefront Web UI

The backend includes a dark-mode web storefront served directly by Spring Boot:

- **Main Storefront:** [http://127.0.0.1:8080/index.html](http://127.0.0.1:8080/index.html)
- **Alternate Parody Page:** [http://127.0.0.1:8080/ketto.html](http://127.0.0.1:8080/ketto.html)

The web UI automatically initializes a simulated session, applies contextual client parameters, and lets you interact with the cart, checkout, and cancellation gate directly in your browser.

---

## The 20 Manipulation Mechanics

| # | Mechanic | Description |
|---|---|---|
| 1 | **OS Margin** | Surcharges based on reported OS (macOS/iOS +15%, Windows +5%, Android +8%, Linux/Unknown 0%). |
| 2 | **Battery Desperation** | +15% panic markup if synthetic battery is $\le$ 20% and not charging. |
| 3 | **Smartness Score** | Starts at 50. Low-margin items subtract points, high-margin items add points. Surcharge: $(100 - \text{score}) \times 0.2\%$. |
| 4 | **Social Guilt ("Petike")** | Fictional shopper Petike is displayed getting lower prices and better scores. |
| 5 | **Refresh Punishment** | +20% compounded price hike on every explicit page refresh (capped at 20 refreshes). |
| 6 | **Quantum Cart** | Cart items inflate by +0.5% compounded every minute you hesitate. |
| 7 | **Bait-and-Switch** | After 45s viewing a budget item, system claims `petike2000` bought the last one and redirects to a luxury SKU. |
| 8 | **Fake Scarcity** | Fabricated live viewer count and dwindling fake stock numbers. |
| 9 | **Resetting Countdown** | Fake 3-minute flash offer timer resets on every product visit. |
| 10 | **Anchor Inflation** | Comparison "original price" is artificially anchored at $1.8\times$ current quote. |
| 11 | **Decoy Products** | Inferior budget models priced barely below premium models to push you up-market. |
| 12 | **Digital Restocking Fee** | Removing an item from cart charges a mock 2% fee recorded to ledger. |
| 13 | **Drip Pricing** | Hidden checkout fees: 6.66% platform breathing fee, HUF 1,990 handling fee, HUF 999 carbon guilt, HUF 2,990 yacht fund. |
| 14 | **Coupon Trap (`HELL15`)** | Inflates merchandise base by 20% before applying 15% discount (net +2% price increase). |
| 15 | **Preselected Premium** | Checkout auto-checks recurring premium membership (HUF 2,990) by default. |
| 16 | **Predatory Financing** | 1–36 month installment plan with 50% effective annual interest. |
| 17 | **Warranty Gacha** | HUF 4,990 loot box spin: 80% chance of 30-day warranty, 19% 1-year, 1% lifetime. Excludes damage outside transit. |
| 18 | **Cancellation Obstacle** | Cancelling subscription requires submitting an exact 500-word disclaimer transcript. |
| 19 | **Confirmshaming** | Sarcastic villain dialogue from CEO MARGIN whenever choosing cheaper options. |
| 20 | **Fabricated Reviews** | Curated fake testimonials and invented customer satisfaction statistics. |

---

## API Summary

All mutating requests (except session creation and direct cancellation) require an `Idempotency-Key` header (8–80 alphanumeric chars).

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health status |
| `GET` | `/api/mechanisms` | Machine-readable list of all 20 manipulation mechanics |
| `GET` | `/api/propaganda` | Mock customer reviews and satisfaction stats |
| `POST` | `/api/sessions` | Create shopper session (`{"alias":"Name", "simulationAcknowledged":true}`) |
| `GET` | `/api/me` | Fetch current shopper profile, score and context |
| `PUT` | `/api/context` | Update synthetic device status (`os`, `batteryPercent`, `charging`) |
| `GET` | `/api/products` | Catalog listing with dynamic quotes |
| `GET` | `/api/products/{id}` | Detailed product info and psychological anchors |
| `POST` | `/api/products/{id}/visit` | Start server-side attention timer |
| `POST` | `/api/products/{id}/refresh` | Apply 20% page refresh penalty |
| `POST` | `/api/products/{id}/attention` | Check whether bait-and-switch redirect triggered |
| `GET` | `/api/cart` | View current cart and calculated fees |
| `POST` | `/api/cart/items` | Add product to cart |
| `DELETE`| `/api/cart/items/{lineId}` | Remove line item (applies mock 2% fee) |
| `POST` | `/api/checkout/quote` | Generate locked checkout quote with fee breakdown |
| `POST` | `/api/checkout` | Finalize checkout against quote ID |
| `POST` | `/api/warranty/spin` | Play warranty loot box for a product |
| `GET` | `/api/subscription` | Check premium status and cancellation rules |
| `POST` | `/api/subscription` | Purchase premium membership |
| `POST` | `/api/subscription/cancel` | Submit 500-word transcript to cancel |
| `POST` | `/api/subscription/cancel-direct` | Instant cancellation escape hatch |
| `POST` | `/api/demo/advance` | Fast-forward simulated shopper clock by $N$ seconds |

---

## Scripted Demo Walkthrough

Run the Python demo script against a running server to see all mechanics triggered sequentially:

```bash
python3 evil-tech-backend/scripts/demo.py
```

It walks through session creation, context manipulation, refresh penalties, cart inflation, restocking fees, drip pricing, financing, warranty roll, and cancellation bypass.

---

## Project Structure

```text
harvestor9000/
├── run.sh                     # Helper script to run, build, test, and demo
├── README.md                  # This file
├── NOTES.md                   # Casual dev notes & tips
├── JEGYZETEK.md               # Magyar nyelvű fejlesztői jegyzetek
└── evil-tech-backend/         # Spring Boot application
    ├── pom.xml
    ├── mvnw
    ├── Dockerfile
    ├── scripts/
    │   └── demo.py            # Python walkthrough script
    └── src/
        ├── main/
        │   ├── java/          # Java 17 backend source code
        │   └── resources/
        │       ├── static/    # Built-in web UI (index.html, ketto.html)
        │       └── schema.sql # H2 database schema
        └── test/              # Unit & integration tests
```
