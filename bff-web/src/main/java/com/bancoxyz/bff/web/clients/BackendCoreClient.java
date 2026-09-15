package com.bancoxyz.bff.web.clients;

import com.bancoxyz.bff.web.dtos.core.CuentaCoreDTO;
import com.bancoxyz.bff.web.dtos.core.TransaccionCoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "backend-core",
        url = "${backend-core.url}"
)
public interface BackendCoreClient {

    @GetMapping("/api/cuentas/{cuentaId}")
    CuentaCoreDTO obtenerCuenta(@PathVariable Long cuentaId);

    @GetMapping("/api/transacciones/cuenta/{cuentaId}")
    List<TransaccionCoreDTO> obtenerTransaccionesPorCuenta(
            @PathVariable Long cuentaId
    );
}