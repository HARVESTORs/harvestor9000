# Fejlesztői jegyzetek: EvilTech (harvestor9000)

Röviden, lényegretörően, AI-bullshit nélkül.

---

## Mi ez a projekt egyáltalán?

Egy Spring Boot 3-as (Java 17) backend egyetemi demonstrációhoz. A lényege: bemutatni a létező összes szemét dark patternt és pszichológiai árazási trükköt, amit egy webshop elkövethet ellened.

**Fontos: minden kamu benne:**
- Nincs igazi bankkártyás fizetés, csak egy belső mock főkönyv.
- Az értékelések, nézőszámok és készletinfók hasraütésre generálódnak.
- Nem hallgatja a mikrofont, nem nézi a valódi akkut — a kliens küld be szintetikus adatokat.
- Az adatbázis egy lokális H2 fájl (`evil-tech-backend/data/evilshop.mv.db`).

---

## Gyorsindítás (TL;DR)

A repo gyökeréből futtatva:

```bash
# 1. Backend fordítása és indítása (8080-as port)
./run.sh start

# 2. Nyisd meg a frontendet a böngészőben:
# http://127.0.0.1:8080/index.html

# 3. Futtasd le a bemutató demó scriptet (külön terminálban):
./run.sh demo
```

Ha kézzel akarod Mavenből:
```bash
cd evil-tech-backend
./mvnw package -DskipTests
java -jar target/evil-tech-backend-1.0.0.jar
```

---

## Fontos tudnivalók és csapdák a repóban

1. **A mappa struktúra:**
   A Java kód, a `mvnw` és a `pom.xml` az `evil-tech-backend/` almappában van. Ha a gyökérből adsz ki `./mvnw` parancsot a segédscript nélkül, hibát kapsz. Használd a `./run.sh`-t vagy lépj be a mappába (`cd evil-tech-backend`).

2. **Bukó tesztek (`PricingEngineTest`):**
   Ha sima `./mvnw test`-et futtatsz, 3 teszt elhasal a `PricingEngineTest`-ben.
   Miért? Valaki viccből átírta a `PricingEngine.java`-ban az értékeket:
   - A `MACOS` / `IOS` szorzó `55.15`-re lett állítva (15% helyett 5415%-os Apple adó).
   - A refresh limit 2000-re, a kosár percek limitje 999999-re lett módosítva.
   Ezért a buildnél érdemes `-DskipTests` flaget használni (a `./run.sh build` ezt automatikusan megteszi).

3. **Adatbázis reset:**
   Ha a kosár vagy a demo adatok elcsúsznak, lődd le a szervert és töröld a data mappát:
   ```bash
   ./run.sh clean-db
   ```
   A következő indításnál tiszta H2 adatbázissal indul.

4. **Idempotency-Key fejléc:**
   Minden POST és DELETE kéréshez (kivéve a session létrehozást és a direkt lemondást) kötelező az `Idempotency-Key` fejléc (8-80 karakter). Ha ugyanazzal a kulccsal más adatot küldesz, `409 Conflict`-ot kapsz. Ha ugyanazt, visszadja az előző választ.

---

## A 20 manipulációs mechanizmus konyhanyelven

1. **Apple adó:** macOS és iOS felhasználók alapból jóval drágábban kapják a termékeket.
2. **Merülő akksi pánik:** Ha az akksid 20% alatt van és nincs töltőn, rádob +15%-ot a sürgősség miatt.
3. **Okossági pontszám (Smartness Score):** 50-ről indul. Ha olcsó dolgot veszel, a CEO hülyének néz és pontot von le, amitől drágul a bolt. Ha drága vackot veszel, pontot kapsz.
4. **Petike bűntudatkeltés:** Egy fiktív vásárló (Petike), aki mindig 15%-kal olcsóbban vásárolt, mint te.
5. **F5 büntetés:** Minden oldalújratöltés +20%-ot tesz az árra.
6. **Ketyegő kosár (Quantum Cart):** Minden percben, amit a kosárban tölt a termék, drágul 0.5%-ot.
7. **Bait & Switch:** Ha 45 másodpercig bámulsz egy olcsó terméket, kiírja, hogy `petike2000` elvitte az utolsót, és átirányít a méregdrágára.
8. **Kamuzott készlet & nézőszám:** "Most 12 ember nézi, már csak 1 darab van raktáron!" (kamu random számok).
9. **Újrainduló 3 perces visszaszámláló:** Minden termékmegnyitáskor újraindul a "villámakció".
10. **Áthúzott kamudrágítás:** Az áthúzott "eredeti" ár mindig a jelenlegi ár 1.8-szorosa.
11. **Csali termékek (Decoy):** Egy használhatatlan gép majdnem annyiba kerül, mint a prémium, hogy megvedd a prémiumot.
12. **Kosár-ürítési büntetés:** Ha kiveszel egy tételt a kosárból, 2% mock büntetődíjat számol fel.
13. **Rejtett költségek (Drip pricing):**
    - 6.66% platform légzési díj
    - 1,990 Ft kezelési költség
    - 999 Ft karbon bűntudat hozzájárulás
    - 2,990 Ft vezérigazgatói jacht alap
14. **Kupon csapda (`HELL15`):** Először megemeli az árat 20%-kal, majd levon belőle 15%-ot. Eredmény: 2%-kal többet fizetsz.
15. **Alattomos előfizetés:** Fizetéskor alapból be van pipálva a havi 2,990 Ft-os VIP tagság.
16. **Uzsorakölcsön:** 1–36 hónapos részletfizetés 50%-os THM-mel.
17. **Garancia szerencsekerék:** 4,990 Ft-ért pörgethetsz. 80% eséllyel csak 30 napot kapsz. A szállítás utáni sérülésekre nem érvényes.
18. **500 szavas lemondó esszé:** A VIP tagságot csak úgy tudod lemondani, ha szóról szóra begépeled a disclaimer szövegét.
19. **Titkos vészkijárat:** Létezik egy rejtett azonnali lemondás végpont (`/api/subscription/cancel-direct`), ami kikerüli az esszét.
20. **MARGIN beszólásai:** A virtuális vezérigazgató szarkasztikus szövegekkel alázza a vásárlót, ha spórolni próbál.

---

## Gyors API próba terminálból

Session készítése:
```bash
curl -s http://127.0.0.1:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"alias":"TesztElek","simulationAcknowledged":true}'
```

Kamu kontextus beállítása (lemerülő MacBook):
```bash
curl -s -X PUT http://127.0.0.1:8080/api/context \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: ctx-001' \
  -d '{"os":"MACOS","batteryPercent":5,"charging":false}'
```
