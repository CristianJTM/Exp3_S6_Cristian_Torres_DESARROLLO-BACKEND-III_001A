package com.bancoxyz.auditoria.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    @GetMapping("/estado")
    public Map<String, String> estado() {

        return Map.of(
                "servicio", "auditoria-service",
                "estado", "activo"
        );
    }
}