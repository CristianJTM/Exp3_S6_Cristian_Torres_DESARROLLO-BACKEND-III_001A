package com.bancoxyz.bff.atm.services;

import com.bancoxyz.bff.atm.clients.BackendCoreClient;
import com.bancoxyz.bff.atm.dtos.RetiroAtmRequestDTO;
import com.bancoxyz.bff.atm.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.atm.exceptions.BackendCoreNoDisponibleException;
import com.bancoxyz.bff.atm.exceptions.CuentaNoEncontradaException;
import com.bancoxyz.bff.atm.exceptions.MontoInvalidoException;
import com.bancoxyz.bff.atm.exceptions.SaldoInsuficienteException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class BackendCoreResilientService {

    private final BackendCoreClient backendCoreClient;

    public BackendCoreResilientService(
            BackendCoreClient backendCoreClient) {

        this.backendCoreClient = backendCoreClient;
    }

    @CircuitBreaker(
            name = "backendCore",
            fallbackMethod = "fallbackObtenerCuenta"
    )
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

    @CircuitBreaker(
            name = "backendCore",
            fallbackMethod = "fallbackRealizarRetiro"
    )
    @Retry(name = "backendCore")
    public CuentaCoreDTO realizarRetiro(
            Long cuentaId,
            RetiroAtmRequestDTO retiro) {

        try {
            return backendCoreClient.realizarRetiro(
                    cuentaId,
                    retiro
            );

        } catch (FeignException.NotFound e) {

            throw new CuentaNoEncontradaException(cuentaId);

        } catch (FeignException.BadRequest e) {

            String respuesta = e.contentUTF8();

            if (respuesta.contains("\"error\":\"Monto inválido\"")) {

                throw new MontoInvalidoException(
                        "El monto del retiro debe ser mayor a cero"
                );
            }

            if (respuesta.contains("\"error\":\"Saldo insuficiente\"")) {

                throw new SaldoInsuficienteException(
                        "Saldo insuficiente para realizar el retiro"
                );
            }

            throw e;

        } catch (FeignException e) {

            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }
    }

    private CuentaCoreDTO fallbackObtenerCuenta(
            Long cuentaId,
            Throwable exception) {

        throw new BackendCoreNoDisponibleException(
                "Backend Core no disponible. No fue posible consultar el saldo."
        );
    }

    private CuentaCoreDTO fallbackRealizarRetiro(
            Long cuentaId,
            RetiroAtmRequestDTO retiro,
            Throwable exception) {

        throw new BackendCoreNoDisponibleException(
                "Backend Core no disponible. No fue posible realizar el retiro."
        );
    }
}