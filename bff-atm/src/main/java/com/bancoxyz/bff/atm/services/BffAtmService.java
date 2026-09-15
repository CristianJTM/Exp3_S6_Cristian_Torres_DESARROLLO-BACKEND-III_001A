package com.bancoxyz.bff.atm.services;

import com.bancoxyz.bff.atm.clients.BackendCoreClient;
import com.bancoxyz.bff.atm.dtos.CuentaAtmDTO;
import com.bancoxyz.bff.atm.dtos.RetiroAtmRequestDTO;
import com.bancoxyz.bff.atm.dtos.RetiroAtmResponseDTO;
import com.bancoxyz.bff.atm.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.atm.exceptions.BackendCoreNoDisponibleException;
import com.bancoxyz.bff.atm.exceptions.CuentaNoEncontradaException;
import com.bancoxyz.bff.atm.exceptions.MontoInvalidoException;
import com.bancoxyz.bff.atm.exceptions.SaldoInsuficienteException;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BffAtmService {

    private final BackendCoreClient backendCoreClient;

    public CuentaAtmDTO consultarSaldo(Long cuentaId) {

        CuentaCoreDTO cuenta;

        try {
            cuenta = backendCoreClient.obtenerCuenta(cuentaId);
        } catch (FeignException.NotFound e) {
            throw new CuentaNoEncontradaException(cuentaId);
        } catch (RetryableException e) {

            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }

        CuentaAtmDTO respuesta = new CuentaAtmDTO();

        respuesta.setCuentaId(cuenta.getCuentaId());
        respuesta.setSaldo(cuenta.getSaldo());

        return respuesta;
    }

    public RetiroAtmResponseDTO realizarRetiro(
            Long cuentaId,
            RetiroAtmRequestDTO retiro) {

        CuentaCoreDTO cuenta;

        try {
            cuenta = backendCoreClient.realizarRetiro(
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

        } catch (RetryableException e) {

            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }

        RetiroAtmResponseDTO respuesta =
                new RetiroAtmResponseDTO();

        respuesta.setCuentaId(cuenta.getCuentaId());
        respuesta.setMontoRetirado(retiro.getMonto());
        respuesta.setSaldo(cuenta.getSaldo());
        respuesta.setEstado("APROBADO");

        return respuesta;
    }
}