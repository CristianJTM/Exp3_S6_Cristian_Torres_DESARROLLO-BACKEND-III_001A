package com.bancoxyz.core.services;

import com.bancoxyz.core.dtos.CuentaDTO;
import com.bancoxyz.core.exceptions.MontoInvalidoException;
import com.bancoxyz.core.exceptions.SaldoInsuficienteException;
import com.bancoxyz.core.model.Cuenta;
import com.bancoxyz.core.exceptions.CuentaNoEncontradaException;
import com.bancoxyz.core.model.Transaccion;
import com.bancoxyz.core.repositories.CuentaRepository;
import com.bancoxyz.core.repositories.TransaccionRepository;
import com.bancoxyz.core.kafka.TransaccionEvento;
import com.bancoxyz.core.kafka.TransaccionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final TransaccionProducer transaccionProducer;

    public CuentaDTO buscarPorId(Long cuentaId) {

        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() ->
                        new CuentaNoEncontradaException(
                                "No se encontró la cuenta con ID: " + cuentaId
                        )
                );

        return convertirADTO(cuenta);
    }

    private CuentaDTO convertirADTO(Cuenta cuenta) {

        CuentaDTO dto = new CuentaDTO();

        dto.setCuentaId(cuenta.getCuentaId());
        dto.setSaldo(cuenta.getSaldo());

        return dto;
    }

    @Transactional
    public CuentaDTO realizarRetiro(
            Long cuentaId,
            BigDecimal monto) {

        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() ->
                        new CuentaNoEncontradaException(
                                "No se encontró la cuenta con ID: " + cuentaId
                        )
                );

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException(
                    "El monto del retiro debe ser mayor a cero"
            );
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente para realizar el retiro"
            );
        }

        cuenta.setSaldo(
                cuenta.getSaldo().subtract(monto)
        );

        cuentaRepository.save(cuenta);

        Transaccion transaccion = new Transaccion();

        Long ultimoId = transaccionRepository.obtenerUltimoId();

        if (ultimoId == null) {
            ultimoId = 0L;
        }

        transaccion.setId(ultimoId + 1);
        transaccion.setCuentaId(cuentaId);
        transaccion.setFecha(LocalDate.now());
        transaccion.setMonto(monto);
        transaccion.setTipo("retiro");

        transaccionRepository.save(transaccion);

        TransaccionEvento evento = new TransaccionEvento(
                "RETIRO_REALIZADO",
                cuentaId,
                monto,
                LocalDateTime.now(),
                cuenta.getSaldo()
        );

        transaccionProducer.publicarRetiro(evento);

        return convertirADTO(cuenta);
    }
}