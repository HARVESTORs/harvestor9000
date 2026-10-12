package edu.evilshop.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ShopException.class) public ResponseEntity<?> shop(ShopException ex) { return error(ex.status,ex.code,ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        String message=ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).sorted().reduce((a,b) -> a+"; "+b).orElse("Invalid request");
        return error(422,"VALIDATION_ERROR",message);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<?> json(HttpMessageNotReadableException ex) {
        return error(400,"INVALID_JSON","Use the documented JSON fields and types; unknown fields, enum values and null primitives are rejected.");
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> arguments(IllegalArgumentException ex) { return error(422,"INVALID_ARGUMENT",ex.getMessage()); }
    private ResponseEntity<?> error(int status,String code,String message) { return ResponseEntity.status(status).body(Map.of("error",code,"message",message,"simulated",true)); }
}
