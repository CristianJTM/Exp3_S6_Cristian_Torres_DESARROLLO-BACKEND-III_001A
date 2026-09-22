package com.bancoxyz.bff.web.services;

import com.bancoxyz.bff.web.clients.BackendCoreClient;
import com.bancoxyz.bff.web.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.web.dtos.core.TransaccionCoreDTO;
import com.bancoxyz.bff.web.exceptions.BackendCoreNoDisponibleException;
import com.bancoxyz.bff.web.exceptions.CuentaNoEncontradaException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackendCoreResilientService {

    private final BackendCoreClient backendCoreClient;

    public BackendCoreResilientService(BackendCoreClient backendCoreClient) {
        this.backendCoreClient = backendCoreClient;
    }

    @CircuitBreaker(name = "backendCore", fallbackMethod = "fallbackCuenta")
    @Retry(name = "backendCore")
    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {

        try {
            return backendCoreClient.obtenerCuenta(cuentaId);

        } catch (FeignException.NotFound e) {
            throw new CuentaNoEncontradaException(cuentaId);

        } catch (FeignException e) {
            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }
    }

    @CircuitBreaker(name = "backendCore", fallbackMethod = "fallbackTransacciones")
    @Retry(name = "backendCore")
    public List<TransaccionCoreDTO> obtenerTransaccionesPorCuenta(
            Long cuentaId) {

        try {
            return backendCoreClient.obtenerTransaccionesPorCuenta(cuentaId);

        } catch (FeignException e) {
            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }
    }

    private CuentaCoreDTO fallbackCuenta(
            Long cuentaId,
            Throwable exception) {

        throw new BackendCoreNoDisponibleException(
                "Backend Core no disponible. No fue posible obtener la cuenta."
        );
    }

    private List<TransaccionCoreDTO> fallbackTransacciones(
            Long cuentaId,
            Throwable exception) {

        throw new BackendCoreNoDisponibleException(
                "Backend Core no disponible. No fue posible obtener las transacciones."
        );
    }
}