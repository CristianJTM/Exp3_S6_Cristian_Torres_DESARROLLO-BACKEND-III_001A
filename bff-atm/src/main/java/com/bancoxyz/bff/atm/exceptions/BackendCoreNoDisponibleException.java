package com.bancoxyz.bff.atm.exceptions;

public class BackendCoreNoDisponibleException extends RuntimeException {

    public BackendCoreNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}