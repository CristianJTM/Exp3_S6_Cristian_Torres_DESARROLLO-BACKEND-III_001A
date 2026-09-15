package com.bancoxyz.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarCuentaNoEncontrada(
            CuentaNoEncontradaException ex) {

        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Cuenta no encontrada",
                "message", ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
    }

    @ExceptionHandler(TransaccionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarTransaccionNoEncontrada(
            TransaccionNoEncontradaException ex) {

        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Transacción no encontrada",
                "message", ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
    }

    @ExceptionHandler(MontoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> manejarMontoInvalido(
            MontoInvalidoException ex) {

        Map<String, Object> respuesta = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", LocalDateTime.now(),
                "message", ex.getMessage(),
                "error", "Monto inválido"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(respuesta);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarSaldoInsuficiente(
            SaldoInsuficienteException ex) {

        Map<String, Object> respuesta = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", LocalDateTime.now(),
                "message", ex.getMessage(),
                "error", "Saldo insuficiente"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(respuesta);
    }
}