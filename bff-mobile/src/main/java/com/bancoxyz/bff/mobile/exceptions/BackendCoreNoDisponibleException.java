package com.bancoxyz.bff.mobile.exceptions;

public class BackendCoreNoDisponibleException extends RuntimeException {

    public BackendCoreNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
