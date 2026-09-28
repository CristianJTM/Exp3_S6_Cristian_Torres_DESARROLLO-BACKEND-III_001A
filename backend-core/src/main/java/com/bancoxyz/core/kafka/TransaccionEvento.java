package com.bancoxyz.core.kafka;

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