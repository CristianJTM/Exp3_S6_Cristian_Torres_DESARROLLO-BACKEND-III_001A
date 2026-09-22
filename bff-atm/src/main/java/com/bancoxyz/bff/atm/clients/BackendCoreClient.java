package com.bancoxyz.bff.atm.clients;

import com.bancoxyz.bff.atm.dtos.RetiroAtmRequestDTO;
import com.bancoxyz.bff.atm.dtos.core.CuentaCoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "backend-core")
public interface BackendCoreClient {

    @GetMapping("/api/cuentas/{cuentaId}")
    CuentaCoreDTO obtenerCuenta(
            @PathVariable Long cuentaId
    );

    @PostMapping("/api/cuentas/{cuentaId}/retiros")
    CuentaCoreDTO realizarRetiro(
            @PathVariable Long cuentaId,
            @RequestBody RetiroAtmRequestDTO retiro
    );
}