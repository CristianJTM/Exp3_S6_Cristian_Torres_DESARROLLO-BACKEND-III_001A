package com.bancoxyz.bff.mobile.services;

import com.bancoxyz.bff.mobile.dtos.CuentaMobileDTO;
import com.bancoxyz.bff.mobile.dtos.MovimientoMobileDTO;
import com.bancoxyz.bff.mobile.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.mobile.dtos.core.TransaccionCoreDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BffMobileService {

    private final BackendCoreResilientService backendCoreService;

    public BffMobileService(
            BackendCoreResilientService backendCoreService) {

        this.backendCoreService = backendCoreService;
    }

    public CuentaMobileDTO obtenerCuenta(Long cuentaId) {

        CuentaCoreDTO cuenta =
                backendCoreService.obtenerCuenta(cuentaId);

        List<TransaccionCoreDTO> transacciones =
                backendCoreService.obtenerTransaccionesPorCuenta(cuentaId);

        List<MovimientoMobileDTO> ultimosMovimientos =
                transacciones.stream()
                        .sorted((t1, t2) ->
                                t2.getFecha().compareTo(t1.getFecha()))
                        .limit(5)
                        .map(this::convertirMovimiento)
                        .toList();

        CuentaMobileDTO cuentaMobile =
                new CuentaMobileDTO();

        cuentaMobile.setCuentaId(cuenta.getCuentaId());
        cuentaMobile.setSaldo(cuenta.getSaldo());
        cuentaMobile.setUltimosMovimientos(ultimosMovimientos);

        return cuentaMobile;
    }

    private MovimientoMobileDTO convertirMovimiento(
            TransaccionCoreDTO transaccion) {

        MovimientoMobileDTO movimiento =
                new MovimientoMobileDTO();

        movimiento.setFecha(transaccion.getFecha());
        movimiento.setMonto(transaccion.getMonto());
        movimiento.setTipo(transaccion.getTipo());

        return movimiento;
    }
}