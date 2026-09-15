package com.bancoxyz.bff.mobile.services;

import com.bancoxyz.bff.mobile.clients.BackendCoreClient;
import com.bancoxyz.bff.mobile.dtos.CuentaMobileDTO;
import com.bancoxyz.bff.mobile.dtos.MovimientoMobileDTO;
import com.bancoxyz.bff.mobile.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.mobile.dtos.core.TransaccionCoreDTO;
import com.bancoxyz.bff.mobile.exceptions.BackendCoreNoDisponibleException;
import com.bancoxyz.bff.mobile.exceptions.CuentaNoEncontradaException;
import feign.RetryableException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BffMobileService {

    private final BackendCoreClient backendCoreClient;

    public BffMobileService(BackendCoreClient backendCoreClient) {
        this.backendCoreClient = backendCoreClient;
    }

    public CuentaMobileDTO obtenerCuenta(Long cuentaId) {

        CuentaCoreDTO cuenta = obtenerCuentaCore(cuentaId);

        List<TransaccionCoreDTO> transacciones =
                backendCoreClient.obtenerTransaccionesPorCuenta(cuentaId);

        List<MovimientoMobileDTO> ultimosMovimientos =
                transacciones.stream()
                        .sorted((t1, t2) -> t2.getFecha().compareTo(t1.getFecha()))
                        .limit(5)
                        .map(this::convertirMovimiento)
                        .toList();

        CuentaMobileDTO cuentaMobile = new CuentaMobileDTO();

        cuentaMobile.setCuentaId(cuenta.getCuentaId());
        cuentaMobile.setSaldo(cuenta.getSaldo());
        cuentaMobile.setUltimosMovimientos(ultimosMovimientos);

        return cuentaMobile;
    }

    private CuentaCoreDTO obtenerCuentaCore(Long cuentaId) {

        try {
            return backendCoreClient.obtenerCuenta(cuentaId);
        } catch (feign.FeignException.NotFound e) {
            throw new CuentaNoEncontradaException(cuentaId);
        }catch (RetryableException e) {
            throw new BackendCoreNoDisponibleException(
                    "No fue posible comunicarse con el Backend Core"
            );
        }
    }

    private MovimientoMobileDTO convertirMovimiento(
            TransaccionCoreDTO transaccion) {

        MovimientoMobileDTO movimiento = new MovimientoMobileDTO();

        movimiento.setFecha(transaccion.getFecha());
        movimiento.setMonto(transaccion.getMonto());
        movimiento.setTipo(transaccion.getTipo());

        return movimiento;
    }
}