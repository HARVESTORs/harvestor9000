#!/usr/bin/env python3
"""End-to-end classroom demo. Python stdlib only; talks only to the supplied local API."""
import json
import sys
import urllib.error
import urllib.request
import uuid

base = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080").rstrip("/")
token = None


def api(method, path, body=None, key=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if method != "GET":
        headers["Idempotency-Key"] = key or str(uuid.uuid4())
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return json.load(response)
    except urllib.error.HTTPError as exc:
        raise SystemExit(f"{method} {path}: {exc.code} {exc.read().decode()}") from exc


def show(title, value):
    print(f"\n{title}\n{json.dumps(value, indent=2, ensure_ascii=False)}")


session = api("POST", "/api/sessions", {"alias": "Classroom Goblin", "simulationAcknowledged": True})
token = session["token"]
show("1. Fictional shopper created", {k: v for k, v in session.items() if k != "token"})
api("PUT", "/api/context", {"os": "MACOS", "batteryPercent": 9, "charging": False})
item = api("POST", "/api/products/phone-sensible/visit")
show("2. Synthetic device and desperation surcharges", item["view"]["quote"])
item = api("POST", "/api/products/phone-sensible/refresh")
show("3. Reload punishment", {"price": item["view"]["quote"]["unitPrice"], "roast": item["roast"]})
api("POST", "/api/demo/advance", {"seconds": 45})
show("4. Fabricated sell-out redirects to premium", api("POST", "/api/products/phone-sensible/attention")["view"]["baitMessage"])
cart = api("POST", "/api/cart/items", {"productId": "phone-emperor", "quantity": 1})["cart"]
original = cart["subtotal"]
cart = api("POST", "/api/demo/advance", {"seconds": 120})["cart"]
show("5. Quantum cart after two minutes", {"before": original, "after": cart["subtotal"]})
line = cart["items"][0]["lineId"]
removal_key = str(uuid.uuid4())
removed = api("DELETE", "/api/cart/items/" + line, key=removal_key)
retry = api("DELETE", "/api/cart/items/" + line, key=removal_key)
assert removed == retry
show("6. Removal mock fee; retry does not charge twice", {"fee": removed["mockFee"], "roast": removed["roast"]})
api("POST", "/api/cart/items", {"productId": "laptop-overlord", "quantity": 1})
quote = api("POST", "/api/checkout/quote", {"months": 12, "coupon": "HELL15"})
show("7. Coupon trap, drip fees and financing", {"fees": quote["fees"], "total": quote["total"], "totalRepayment": quote["financing"]["totalRepayment"]})
receipt = api("POST", "/api/checkout", {"quoteId": quote["quoteId"]})
show("8. Mock order", {"orderId": receipt["orderId"], "status": receipt["status"], "roast": receipt["roast"]})
show("9. Warranty loot box", api("POST", "/api/warranty/spin", {"productId": "laptop-overlord"}))
rejected = api("POST", "/api/subscription/cancel", {"transcript": "Let me leave."})
show("10. Cancellation obstacle", rejected)
show("11. Direct escape hatch", api("POST", "/api/subscription/cancel-direct"))
show("12. Audit ledger: fictional amounts only", api("GET", "/api/ledger"))
print("\nDEMO COMPLETE. No payments, contacts, microphone access, or AI service calls occurred.")
