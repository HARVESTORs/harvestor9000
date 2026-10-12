package edu.evilshop.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.evilshop.api.ShopException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class SessionStore {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final Clock clock;
    private final int sessionLimit;
    private final SecureRandom random = new SecureRandom();
    public SessionStore(JdbcTemplate db, TransactionTemplate tx, ObjectMapper json, Clock clock, @Value("${shop.session-limit}") int sessionLimit) {
        this.db=db; this.tx=tx; this.json=json; this.clock=clock; this.sessionLimit=sessionLimit;
    }
    public synchronized Map<String, Object> create(String alias) {
        if (db.queryForObject("SELECT COUNT(*) FROM shop_session", Integer.class) >= sessionLimit)
            throw new ShopException(429, "SESSION_LIMIT", "The demo reached its 1000-session limit. Reset the local database to start fresh.");
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ShopState state=new ShopState(); state.id=UUID.randomUUID().toString(); state.alias=alias; state.createdAt=clock.instant();
        db.update("INSERT INTO shop_session(id, token_hash, state) VALUES(?,?,?)", state.id, hash(token), encode(state));
        return Map.of("sessionId",state.id,"token",token,"alias",alias,"smartnessScore",state.smartnessScore,
                "simulated",true,"notice","University simulation: fictional identities, fabricated persuasion, mock payments. Keep the bearer token to resume this shopper.");
    }
    public ShopState read(String token) { return load(hash(token), false); }
    public <T> T update(String token, Function<ShopState,T> operation) {
        String hashed=hash(token);
        return tx.execute(ignored -> {
            ShopState state=load(hashed,true);
            T result=operation.apply(state);
            db.update("UPDATE shop_session SET state=? WHERE id=?", encode(state),state.id);
            return result;
        });
    }
    public void eachSession(java.util.function.Consumer<ShopState> operation) {
        for (String id:db.queryForList("SELECT id FROM shop_session",String.class)) {
            tx.executeWithoutResult(ignored -> {
                List<String> rows=db.queryForList("SELECT state FROM shop_session WHERE id=? FOR UPDATE",String.class,id);
                if (rows.isEmpty()) return;
                ShopState state=decode(rows.get(0)); operation.accept(state);
                db.update("UPDATE shop_session SET state=? WHERE id=?",encode(state),id);
            });
        }
    }
    private ShopState load(String hash,boolean lock) {
        List<String> rows=db.queryForList("SELECT state FROM shop_session WHERE token_hash=?"+(lock?" FOR UPDATE":""),String.class,hash);
        if (rows.isEmpty()) throw new ShopException(401,"INVALID_SESSION","Supply the bearer token from POST /api/sessions.");
        return decode(rows.get(0));
    }
    public static String hash(String token) {
        if (token==null || token.isBlank()) throw new ShopException(401,"MISSING_SESSION","A demo bearer token is required.");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private String encode(ShopState state) {
        try { return json.writeValueAsString(state); } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot save demo state",ex); }
    }
    private ShopState decode(String value) {
        try { return json.readValue(value,ShopState.class); } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read demo state",ex); }
    }
}
