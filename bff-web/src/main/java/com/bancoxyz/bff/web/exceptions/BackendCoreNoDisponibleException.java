package com.bancoxyz.bff.web.exceptions;

public class BackendCoreNoDisponibleException extends RuntimeException {

    public BackendCoreNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
