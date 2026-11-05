package edu.evilshop;

import com.fasterxml.jackson.databind.*;
import edu.evilshop.api.ShopException;
import edu.evilshop.store.SessionStore;
import edu.evilshop.store.ShopService;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:eviltest;DB_CLOSE_DELAY=-1", "shop.inflation-tick-ms=3600000"})
@AutoConfigureMockMvc
class ShopApiTest {
    @TestConfiguration static class TimeConfig { @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"),ZoneOffset.UTC); } }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired SessionStore store;
    @Autowired ShopService shop;

    private String key() { return UUID.randomUUID().toString(); }
    private JsonNode call(String method,String path,String token,String key,Object body,int status) throws Exception {
        var request=MockMvcRequestBuilders.request(HttpMethod.valueOf(method),path);
        if (token!=null) request.header("Authorization","Bearer "+token);
        if (key!=null) request.header("Idempotency-Key",key);
        if (body!=null) request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        var response=mvc.perform(request).andExpect(status().is(status)).andExpect(header().string("X-Simulation","university-demo")).andReturn().getResponse();
        return mapper.readTree(response.getContentAsString());
    }
    private String session() throws Exception { return call("POST","/api/sessions",null,null,Map.of("alias","Test Shopper","simulationAcknowledged",true),201).path("token").asText(); }
    private JsonNode get(String path,String token) throws Exception { return call("GET",path,token,null,null,200); }
    private JsonNode post(String path,String token,Object body) throws Exception { return call("POST",path,token,key(),body,200); }
    private JsonNode add(String token,String product) throws Exception { return post("/api/cart/items",token,Map.of("productId",product,"quantity",1)); }
    private void advance(String token,long seconds) throws Exception { post("/api/demo/advance",token,Map.of("seconds",seconds)); }
    private BigDecimal amount(JsonNode value) { return new BigDecimal(value.asText()).setScale(2,RoundingMode.HALF_UP); }

    @Test void healthAndConsentAndAuthentication() throws Exception {
        assertEquals("MOCK_ONLY",get("/api/health",null).path("payments").asText());
        call("POST","/api/sessions",null,null,Map.of("alias","Student","simulationAcknowledged",false),422);
        call("GET","/api/cart",null,null,null,401);
        call("GET","/api/cart","a".repeat(43),null,null,401);
        String token=session();
        assertEquals(50,get("/api/me",token).path("smartnessScore").asInt());
        assertEquals(20,get("/api/mechanisms",null).path("implemented").size());
    }
    @Test void strictInputsAndSizeLimit() throws Exception {
        String token=session();
        call("POST","/api/cart/items",token,key(),Map.of("productId","phone-sensible","quantity",0),422);
        call("POST","/api/cart/items",token,key(),Map.of("productId","phone-sensible","quantity","2"),400);
        call("POST","/api/cart/items",token,key(),Map.of("productId","phone-sensible","quantity",1,"cardNumber","not-accepted"),400);
        call("PUT","/api/context",token,key(),Map.of("os","LINUX","batteryPercent",101,"charging",false),422);
        call("POST","/api/cart/items",token,null,Map.of("productId","phone-sensible","quantity",1),400);
        call("POST","/api/cart/items",token,key(),Map.of("productId","nonexistent","quantity",1),404);
        call("POST","/api/subscription/cancel",token,key(),Map.of("transcript","x".repeat(33000)),413);
    }
    @Test void productClassesContextRefreshAndScore() throws Exception {
        String token=session();
        JsonNode laptop=get("/api/products/laptop-sensible",token);
        assertEquals("LAPTOP",laptop.at("/product/type").asText());
        assertEquals(512,laptop.at("/product/ssdGb").asInt());
        call("PUT","/api/context",token,key(),Map.of("os","MACOS","batteryPercent",10,"charging",false),200);
        BigDecimal before=amount(get("/api/products/laptop-sensible",token).at("/quote/unitPrice"));
        String replayKey=key();
        JsonNode first=call("POST","/api/products/laptop-sensible/refresh",token,replayKey,null,200);
        assertEquals(before.multiply(new BigDecimal("1.20")).setScale(2,RoundingMode.HALF_UP),amount(first.at("/view/quote/unitPrice")));
        assertEquals(first,call("POST","/api/products/laptop-sensible/refresh",token,replayKey,null,200));
        JsonNode added=add(token,"laptop-sensible");
        assertEquals(45,added.path("smartnessScore").asInt());
        assertTrue(added.at("/roast/text").asText().length()>10);
        assertEquals(53,add(token,"phone-emperor").path("smartnessScore").asInt());
    }
    @Test void quantumCartMinuteBoundariesAndSchedulerDoNotDoubleCompound() throws Exception {
        String token=session(); add(token,"phone-emperor");
        BigDecimal start=amount(get("/api/cart",token).path("subtotal"));
        advance(token,59);
        assertEquals(start,amount(get("/api/cart",token).path("subtotal")));
        advance(token,1);
        BigDecimal one=start.multiply(new BigDecimal("1.005")).setScale(2,RoundingMode.HALF_UP);
        assertEquals(one,amount(get("/api/cart",token).path("subtotal")));
        int events=get("/api/events",token).path("events").size();
        shop.backgroundTick(); shop.backgroundTick();
        assertEquals(one,amount(get("/api/cart",token).path("subtotal")));
        assertEquals(events,get("/api/events",token).path("events").size());
    }
    @Test void dayOldCartRoundTripsPersistedStateAndPriceCap() throws Exception {
        String token=session(); add(token,"phone-emperor");
        advance(token,86400);
        JsonNode aged=get("/api/cart",token);
        assertEquals(1440,aged.at("/items/0/elapsedMinutes").asInt());
        post("/api/checkout/quote",token,Map.of("months",12,"includePremium",false));
        // Deserializes stored events, quote, and idempotency responses with compound multipliers.
        assertNotNull(store.read(token));
        advance(token,86400);
        assertEquals(amount(aged.path("subtotal")),amount(get("/api/cart",token).path("subtotal")));
    }
    @Test void baitSwitchAt45SecondsAndNoGlobalStockMutation() throws Exception {
        String token=session(); post("/api/products/phone-sensible/visit",token,null);
        advance(token,44);
        assertEquals("WAIT",post("/api/products/phone-sensible/attention",token,null).path("action").asText());
        advance(token,1);
        assertEquals("/api/products/phone-emperor",post("/api/products/phone-sensible/attention",token,null).path("redirectTo").asText());
        call("POST","/api/cart/items",token,key(),Map.of("productId","phone-sensible","quantity",1),409);
        assertEquals("DISPLAY",get("/api/products/phone-sensible",session()).path("action").asText());
    }
    @Test void removalFeeAndIdempotencyPreventDoubleCharge() throws Exception {
        String token=session(); JsonNode cart=add(token,"phone-emperor").path("cart");
        String line=cart.at("/items/0/lineId").asText(), replayKey=key();
        String path="/api/cart/items/"+line;
        JsonNode first=call("DELETE",path,token,replayKey,null,200);
        assertEquals(amount(cart.path("subtotal")).multiply(new BigDecimal("0.02")).setScale(2,RoundingMode.HALF_UP),amount(first.path("mockFee")));
        assertEquals(first,call("DELETE",path,token,replayKey,null,200));
        assertEquals(1,get("/api/ledger",token).path("entries").size());
        call("POST","/api/cart/items",token,replayKey,Map.of("productId","phone-sensible","quantity",1),409);
        assertEquals(0,get("/api/cart",token).path("items").size());
    }
    @Test void couponFinancingAndDisclosedFeeTotals() throws Exception {
        String token=session(); add(token,"laptop-overlord");
        JsonNode quote=post("/api/checkout/quote",token,Map.of("months",12,"coupon","HELL15","includePremium",false));
        BigDecimal subtotal=amount(quote.at("/cart/subtotal"));
        BigDecimal feeSum=BigDecimal.ZERO;
        for (JsonNode fee:quote.path("fees")) feeSum=feeSum.add(amount(fee.path("amount")));
        assertEquals(subtotal.add(feeSum),amount(quote.path("total")));
        assertEquals(subtotal.multiply(new BigDecimal("0.02")).setScale(2,RoundingMode.HALF_UP),amount(quote.at("/fees/0/amount")).add(amount(quote.at("/fees/1/amount"))));
        assertEquals(12,quote.at("/financing/schedule").size());
        assertEquals(new BigDecimal("0.00"),amount(quote.at("/financing/schedule/11/remainingBalance")));
        assertEquals(new BigDecimal("0.50"),amount(quote.at("/financing/annualEffectiveRate")));
    }
    @Test void checkoutPersistsReceiptAndRetriesExactlyOnce() throws Exception {
        String token=session(); add(token,"phone-emperor");
        JsonNode quote=post("/api/checkout/quote",token,Map.of("months",12));
        assertTrue(quote.path("premiumSelected").asBoolean());
        String replayKey=key(); Map<String,String> body=Map.of("quoteId",quote.path("quoteId").asText());
        JsonNode receipt=call("POST","/api/checkout",token,replayKey,body,200);
        assertEquals(receipt,call("POST","/api/checkout",token,replayKey,body,200));
        assertEquals(1,get("/api/orders",token).path("orders").size());
        assertEquals(0,get("/api/cart",token).path("items").size());
        JsonNode ledger=get("/api/ledger",token).path("entries");
        assertEquals(1,ledger.size());
        assertEquals("MOCK_FINANCE_OBLIGATION",ledger.get(0).path("kind").asText());
        assertEquals(amount(quote.at("/financing/totalRepayment")),amount(ledger.get(0).path("amount")));
        call("POST","/api/checkout",token,key(),body,409);
        call("POST","/api/checkout",session(),key(),body,409);
    }
    @Test void quotesExpireAndCartChangesInvalidate() throws Exception {
        String token=session(); add(token,"phone-emperor");
        JsonNode quote=post("/api/checkout/quote",token,Map.of("months",0,"includePremium",false));
        advance(token,60);
        assertEquals("QUOTE_EXPIRED",call("POST","/api/checkout",token,key(),Map.of("quoteId",quote.path("quoteId").asText()),409).path("error").asText());
        assertEquals(0,get("/api/ledger",token).path("entries").size());
        quote=post("/api/checkout/quote",token,Map.of("months",0));
        add(token,"laptop-overlord");
        call("POST","/api/checkout",token,key(),Map.of("quoteId",quote.path("quoteId").asText()),409);
    }
    @Test void premiumChangesInvalidateQuotesAndGateRequiresExactTranscript() throws Exception {
        String token=session(); add(token,"phone-emperor");
        JsonNode quote=post("/api/checkout/quote",token,Map.of("months",0));
        post("/api/subscription",token,null);
        call("POST","/api/checkout",token,key(),Map.of("quoteId",quote.path("quoteId").asText()),409);
        JsonNode wrong=post("/api/subscription/cancel",token,Map.of("transcript","no ".repeat(500)));
        assertTrue(wrong.path("active").asBoolean()); assertFalse(wrong.path("cancelled").asBoolean());
        String transcript=get("/api/subscription",token).at("/challenge/disclaimer").asText();
        assertTrue(post("/api/subscription/cancel",token,Map.of("transcript",transcript)).path("cancelled").asBoolean());
        post("/api/subscription",token,null);
        quote=post("/api/checkout/quote",token,Map.of("months",0));
        call("POST","/api/subscription/cancel-direct",token,null,null,200);
        call("POST","/api/subscription/cancel-direct",token,null,null,200);
        call("POST","/api/checkout",token,key(),Map.of("quoteId",quote.path("quoteId").asText()),409);
        assertFalse(get("/api/subscription",token).path("active").asBoolean());
    }
    @Test void warrantyReplayReturnsSamePrizeAndOneFee() throws Exception {
        String token=session(), replayKey=key(); Map<String,String> body=Map.of("productId","phone-sensible");
        JsonNode first=call("POST","/api/warranty/spin",token,replayKey,body,200);
        assertEquals(first,call("POST","/api/warranty/spin",token,replayKey,body,200));
        assertEquals(new BigDecimal("4990.00"),amount(first.path("mockFee")));
        assertEquals(1,get("/api/ledger",token).path("entries").size());
        assertTrue(first.at("/prize/simulated").asBoolean());
    }
    @Test void concurrentSameKeyCannotDoubleDebit() throws Exception {
        String token=session(), replayKey=key();
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try {
            List<Callable<JsonNode>> tasks=new ArrayList<>();
            for (int i=0;i<4;i++) tasks.add(() -> call("POST","/api/warranty/spin",token,replayKey,Map.of("productId","phone-sensible"),200));
            var results=pool.invokeAll(tasks); JsonNode first=results.get(0).get();
            for (var result:results) assertEquals(first,result.get());
        } finally { pool.shutdownNow(); }
        assertEquals(1,get("/api/ledger",token).path("entries").size());
    }
    @Test void transactionFailureRollsBackAggregate() throws Exception {
        String token=session();
        assertThrows(ShopException.class,() -> store.update(token,state -> { state.smartnessScore=0; throw new ShopException(409,"TEST_ABORT","rollback"); }));
        assertEquals(50,store.read(token).smartnessScore);
    }
}
