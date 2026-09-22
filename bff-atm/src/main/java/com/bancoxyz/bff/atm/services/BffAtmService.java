package com.bancoxyz.bff.atm.services;

import com.bancoxyz.bff.atm.dtos.CuentaAtmDTO;
import com.bancoxyz.bff.atm.dtos.RetiroAtmRequestDTO;
import com.bancoxyz.bff.atm.dtos.RetiroAtmResponseDTO;
import com.bancoxyz.bff.atm.dtos.core.CuentaCoreDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BffAtmService {

    private final BackendCoreResilientService backendCoreService;

    public CuentaAtmDTO consultarSaldo(Long cuentaId) {

        CuentaCoreDTO cuenta =
                backendCoreService.obtenerCuenta(cuentaId);

        CuentaAtmDTO respuesta =
                new CuentaAtmDTO();

        respuesta.setCuentaId(cuenta.getCuentaId());
        respuesta.setSaldo(cuenta.getSaldo());

        return respuesta;
    }

    public RetiroAtmResponseDTO realizarRetiro(
            Long cuentaId,
            RetiroAtmRequestDTO retiro) {

        CuentaCoreDTO cuenta =
                backendCoreService.realizarRetiro(
                        cuentaId,
                        retiro
                );

        RetiroAtmResponseDTO respuesta =
                new RetiroAtmResponseDTO();

        respuesta.setCuentaId(cuenta.getCuentaId());
        respuesta.setMontoRetirado(retiro.getMonto());
        respuesta.setSaldo(cuenta.getSaldo());
        respuesta.setEstado("APROBADO");

        return respuesta;
    }
}