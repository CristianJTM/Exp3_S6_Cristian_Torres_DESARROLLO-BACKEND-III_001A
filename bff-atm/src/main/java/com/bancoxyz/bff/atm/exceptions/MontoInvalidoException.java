package com.bancoxyz.bff.atm.exceptions;

public class MontoInvalidoException extends RuntimeException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}