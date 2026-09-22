package com.bancoxyz.bff.web.services;

import com.bancoxyz.bff.web.dtos.CuentaWebDetalleDTO;
import com.bancoxyz.bff.web.dtos.CuentaWebResumenDTO;
import com.bancoxyz.bff.web.dtos.MovimientoWebDTO;
import com.bancoxyz.bff.web.dtos.ResumenMovimientosDTO;
import com.bancoxyz.bff.web.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.web.dtos.core.TransaccionCoreDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BffWebService {

    private final BackendCoreResilientService backendCoreService;

    public BffWebService(
            BackendCoreResilientService backendCoreService) {

        this.backendCoreService = backendCoreService;
    }

    public CuentaWebDetalleDTO obtenerDetalleCuenta(Long cuentaId) {

        CuentaCoreDTO cuenta =
                backendCoreService.obtenerCuenta(cuentaId);

        List<TransaccionCoreDTO> transacciones =
                backendCoreService.obtenerTransaccionesPorCuenta(cuentaId);

        ResumenMovimientosDTO resumenMovimientos =
                construirResumen(transacciones);

        resumenMovimientos.setSaldo(cuenta.getSaldo());

        CuentaWebResumenDTO resumen =
                new CuentaWebResumenDTO();

        resumen.setCuentaId(cuenta.getCuentaId());
        resumen.setResumen(resumenMovimientos);

        List<MovimientoWebDTO> movimientos =
                transacciones.stream()
                        .map(this::convertirMovimiento)
                        .toList();

        CuentaWebDetalleDTO detalle =
                new CuentaWebDetalleDTO();

        detalle.setCuentaId(cuenta.getCuentaId());
        detalle.setResumen(resumen);
        detalle.setMovimientos(movimientos);

        return detalle;
    }

    public CuentaWebResumenDTO obtenerResumenCuenta(Long cuentaId) {

        CuentaCoreDTO cuenta =
                backendCoreService.obtenerCuenta(cuentaId);

        List<TransaccionCoreDTO> transacciones =
                backendCoreService.obtenerTransaccionesPorCuenta(cuentaId);

        ResumenMovimientosDTO resumenMovimientos =
                construirResumen(transacciones);

        resumenMovimientos.setSaldo(cuenta.getSaldo());

        CuentaWebResumenDTO resumen =
                new CuentaWebResumenDTO();

        resumen.setCuentaId(cuenta.getCuentaId());
        resumen.setResumen(resumenMovimientos);

        return resumen;
    }

    private ResumenMovimientosDTO construirResumen(
            List<TransaccionCoreDTO> transacciones) {

        BigDecimal totalDepositos =
                transacciones.stream()
                        .filter(t ->
                                "deposito".equalsIgnoreCase(t.getTipo()))
                        .map(TransaccionCoreDTO::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetiros =
                transacciones.stream()
                        .filter(t ->
                                !"deposito".equalsIgnoreCase(t.getTipo()))
                        .map(TransaccionCoreDTO::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        ResumenMovimientosDTO resumen =
                new ResumenMovimientosDTO();

        resumen.setTotalMovimientos(transacciones.size());
        resumen.setTotalDepositos(totalDepositos);
        resumen.setTotalRetiros(totalRetiros);

        return resumen;
    }

    private MovimientoWebDTO convertirMovimiento(
            TransaccionCoreDTO transaccion) {

        MovimientoWebDTO movimiento =
                new MovimientoWebDTO();

        movimiento.setId(transaccion.getId());
        movimiento.setFecha(transaccion.getFecha());
        movimiento.setMonto(transaccion.getMonto());
        movimiento.setTipo(transaccion.getTipo());

        return movimiento;
    }
}