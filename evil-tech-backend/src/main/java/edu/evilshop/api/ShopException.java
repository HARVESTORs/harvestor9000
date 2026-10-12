package edu.evilshop.api;

public class ShopException extends RuntimeException {
    public final int status;
    public final String code;
    public ShopException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
}
