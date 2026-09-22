package com.bancoxyz.bff.mobile.clients;

import com.bancoxyz.bff.mobile.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.mobile.dtos.core.TransaccionCoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "backend-core")
public interface BackendCoreClient {

    @GetMapping("/api/cuentas/{cuentaId}")
    CuentaCoreDTO obtenerCuenta(@PathVariable Long cuentaId);

    @GetMapping("/api/transacciones/{id}")
    TransaccionCoreDTO obtenerTransaccion(@PathVariable Long id);

    @GetMapping("/api/transacciones/cuenta/{cuentaId}")
    List<TransaccionCoreDTO> obtenerTransaccionesPorCuenta(
            @PathVariable Long cuentaId
    );
}
