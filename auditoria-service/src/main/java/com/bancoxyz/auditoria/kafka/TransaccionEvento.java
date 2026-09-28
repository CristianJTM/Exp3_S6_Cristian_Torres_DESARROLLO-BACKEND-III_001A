package com.bancoxyz.auditoria.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionEvento(
        String evento,
        Long cuentaId,
        BigDecimal monto,
        LocalDateTime fecha,
        BigDecimal saldoPosterior
) {
}
